package com.ebay.chris.server;

import com.cedarsoftware.util.io.JsonReader;
import com.cedarsoftware.util.io.JsonWriter;
import com.ebay.chris.common.Storage;
import com.ebay.chris.common.Util;
import com.ebay.chris.model.Order;
import org.apache.log4j.Logger;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class Engine {
    private static final int failedRate = 5;
    private static final int maxProcessTime = 5;
    private static final Jedis jedis = Util.jedis();

    private static final Logger logger = Logger.getLogger(Engine.class);

    public void process() {
        Step step = Step.CREATED;
        while (!step.equals(Step.COMPLETED) && !step.equals(Step.FAILED)) {
            step.setStartAt(Instant.now().getEpochSecond());
            step = step.next();
        }
    }

    public enum Step {
        // Fake Steps
        CREATED {
            @Override
            public Step next() {
                String orderInfo = jedis.rpoplpush(Storage.stageQueue, Storage.stageQueue + ":processing");
                Order order = (Order) JsonReader.jsonToJava(orderInfo);

                order.setAcceptedAt(Instant.now().getEpochSecond());
                order.getCurrentStep().setStartAt(Instant.now().getEpochSecond());

                Transaction t = jedis.multi();
                t.lpush(Storage.schedulingQueue, JsonWriter.objectToJson(order));
                t.lrem(Storage.stageQueue + ":processing", 1, orderInfo);

                this.setEndAt(Instant.now().getEpochSecond());
                logger.debug("Processing @Step: " + this);
                return SCHEDULING;
            }
        },

        // Process Steps
        SCHEDULING {
            @Override
            public Step next() {
                long begin = Instant.now().getEpochSecond();
                Map<String, Order> data = reliableQueueProcessing(Storage.schedulingQueue, SCHEDULING);
                Transaction t = jedis.multi();
                t.lpush(Storage.preProcessingQueue, JsonWriter.objectToJson(data.values().iterator().next()));
                t.lrem(Storage.schedulingQueue + ":processing", 1, data.keySet().iterator().next());
                t.exec();

                this.setEndAt(Instant.now().getEpochSecond());
                int gap = (int)(maxProcessTime - (this.getEndAt() - begin));
                Util.sleep(gap);
                boolean willRollback = false;
                if (willRollback = isFailed()) {
                    t.discard();
                } else {
                    t.exec();
                    this.setEndAt(Instant.now().getEpochSecond());
                }
                logger.debug("Processing @Step: " + this);
                return willRollback?FAILED:PRE_PROCESSING;
            }
        },

        PRE_PROCESSING {
            @Override
            public Step next() {
                long begin = Instant.now().getEpochSecond();
                Map<String, Order> data = reliableQueueProcessing(Storage.preProcessingQueue, PRE_PROCESSING);
                Transaction t = jedis.multi();
                t.lpush(Storage.processingQueue, JsonWriter.objectToJson(data.values().iterator().next()));
                t.lrem(Storage.preProcessingQueue + ":processing", 1, data.keySet().iterator().next());
                t.exec();

                this.setEndAt(Instant.now().getEpochSecond());
                int gap = (int)(maxProcessTime - (this.getEndAt() - begin));
                Util.sleep(gap);
                boolean willRollback = false;
                if (willRollback = isFailed()) {
                    t.discard();
                } else {
                    t.exec();
                    this.setEndAt(Instant.now().getEpochSecond());
                }
                this.setEndAt(Instant.now().getEpochSecond());
                logger.debug("Processing @Step: " + this);
                return willRollback?FAILED:PROCESSING;
            }
        },

        PROCESSING {
            @Override
            public Step next() {
                long begin = Instant.now().getEpochSecond();
                Map<String, Order> data = reliableQueueProcessing(Storage.processingQueue, PROCESSING);
                Transaction t = jedis.multi();
                t.lpush(Storage.postProcessingQueue, JsonWriter.objectToJson(data.values().iterator().next()));
                t.lrem(Storage.processingQueue+ ":processing", 1, data.keySet().iterator().next());
                t.exec();

                this.setEndAt(Instant.now().getEpochSecond());
                int gap = (int)(maxProcessTime - (this.getEndAt() - begin));
                Util.sleep(gap);
                boolean willRollback = false;
                if (willRollback = isFailed()) {
                    t.discard();
                } else {
                    t.exec();
                    this.setEndAt(Instant.now().getEpochSecond());
                }
                this.setEndAt(Instant.now().getEpochSecond());
                logger.debug("Processing @Step: " + this);
                return willRollback?FAILED:POSTPROCESSING;
            }
        },
        POSTPROCESSING {
            @Override
            public Step next() {
                long begin = Instant.now().getEpochSecond();
                Map<String, Order> data = reliableQueueProcessing(Storage.postProcessingQueue, PROCESSING);
                Transaction t = jedis.multi();
                t.lpush(Storage.completeQueue, JsonWriter.objectToJson(data.values().iterator().next()));
                t.lrem(Storage.postProcessingQueue+ ":processing", 1, data.keySet().iterator().next());
                t.exec();

                this.setEndAt(Instant.now().getEpochSecond());
                int gap = (int)(maxProcessTime - (this.getEndAt() - begin));
                Util.sleep(gap);
                boolean willRollback = false;
                if (willRollback = isFailed()) {
                    t.discard();
                } else {
                    t.exec();
                    this.setEndAt(Instant.now().getEpochSecond());
                }
                this.setEndAt(Instant.now().getEpochSecond());
                logger.debug("Processing @Step: " + this);

                return willRollback?FAILED:COMPLETED;
            }
        },
        COMPLETED {
        },
        FAILED {

        };

        private long startAt = 0L;
        private long endAt = 0L;

        public Step next() {
            return COMPLETED;
        }

        public long getStartAt() {
            return startAt;
        }

        public void setStartAt(long startAt) {
            this.startAt = startAt;
        }

        public long getEndAt() {
            return endAt;
        }

        public void setEndAt(long endAt) {
            this.endAt = endAt;
        }

        public Map<String, Order> reliableQueueProcessing(String srcQueue, Step currentStep) {
            long beginAt = Instant.now().getEpochSecond();

            String orderInfo = jedis.rpoplpush(srcQueue, srcQueue + ":processing");
            Order order = (Order) JsonReader.jsonToJava(orderInfo);
            order.setCurrentStep(currentStep);
            order.getCurrentStep().setStartAt(beginAt);
            order.getCurrentStep().setEndAt(Instant.now().getEpochSecond());
            Map<String, Order> map = new HashMap<>();
            map.put(orderInfo, order);
            return map;
        }

        public boolean isFailed() {
            Random random = new Random();
            return random.nextInt(100) < failedRate;
        }

        @Override
        public String toString() {
            return super.toString() +
                    "{" +
                    "startAt='" + startAt + '\'' +
                    ", endAt='" + endAt + '\'' +
                    '}';
        }
    }
}
