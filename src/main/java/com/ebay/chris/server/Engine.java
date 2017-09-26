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
    private static Random random = new Random();
    private static final Logger logger = Logger.getLogger(Engine.class);

    public void process() {
        logger.debug("processor: " + Thread.currentThread().getName());
        while (true) {
            Map<String, Order> orderInfo = pollOrder();
            String info = orderInfo.keySet().iterator().next();
            Order order = orderInfo.get(info);
            Step step = order.getCurrentStep();

            step.setStartAt(Instant.now().getEpochSecond());
            step.next(info, order);
        }
    }

     public Map<String, Order> pollOrder() {
            Map<String, Order> map = new HashMap<>();
            for (;map.isEmpty();) {
                int idx = random.nextInt(Storage.queues.length);
                String queue = Storage.queues[idx];
                if (!queue.equals(Storage.completeQueue)) {
                    try {
                        map = reliableQueuePoll(queue);
                    } catch (Exception e) {
                        Util.sleep(5);
                    }
                }
            }
            return map;
        }

    public static Map<String, Order> reliableQueueProcessing(String srcQueue, Step currentStep) {
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

    public static Map<String, Order> reliableQueuePoll(String queue) {
        Map<String, Order> map = new HashMap<>();
        String orderInfo = jedis.rpoplpush(queue, queue+ ":processing");
        if (orderInfo != null) {
            Order order = (Order) JsonReader.jsonToJava(orderInfo);
            map.put(orderInfo, order);
        }
        return map;
    }

    public enum Step {
        // Fake Steps
        CREATED {
            @Override
            public Step next(String originOrderInfo, Order order) {
                super.next(originOrderInfo, order);
                Transaction t = jedis.multi();
                t.lpush(Storage.schedulingQueue, JsonWriter.objectToJson(order));
                t.lrem(Storage.stageQueue + ":processing", 1, originOrderInfo);

                this.setEndAt(Instant.now().getEpochSecond());
                logger.debug("Processing @Step: " + this);
                return SCHEDULING;
            }
        },

        // Process Steps
        SCHEDULING {
            @Override
            public Step next(String originOrderInfo, Order order) {
                super.next(originOrderInfo, order);

                long begin = Instant.now().getEpochSecond();
                Transaction t = jedis.multi();
                t.lpush(Storage.preProcessingQueue, JsonWriter.objectToJson(order));
                t.lrem(Storage.schedulingQueue + ":processing", 1, originOrderInfo);
                t.exec();

                this.setEndAt(Instant.now().getEpochSecond());
                int gap = (int)(maxProcessTime - (this.getEndAt() - begin));
                Util.sleep(gap);
                boolean willRollback = false;
                if (willRollback = isFailed()) {
                    rollback(order);
                } else {
                    t.exec();
                    this.setEndAt(Instant.now().getEpochSecond());
                }
                logger.debug("Processing @Step: " + this);
                return willRollback?FAILED:PRE_PROCESSING;
            }

            @Override
            public void rollback(Order order) {
                super.rollback(order);
                jedis.lrem(Storage.preProcessingQueue, 1, JsonWriter.objectToJson(order));
                order.setCurrentStep(FAILED);
                order.getCurrentStep().setStartAt(0L);
                order.getCurrentStep().setEndAt(0L);
                jedis.lpush(Storage.schedulingQueue, JsonWriter.objectToJson(order));
            }
        },

        PRE_PROCESSING {
            @Override
            public Step next(String originOrderInfo, Order order) {
                super.next(originOrderInfo, order);

                long begin = Instant.now().getEpochSecond();
                Transaction t = jedis.multi();
                t.lpush(Storage.processingQueue, JsonWriter.objectToJson(order));
                t.lrem(Storage.preProcessingQueue + ":processing", 1, originOrderInfo);
                t.exec();

                this.setEndAt(Instant.now().getEpochSecond());
                int gap = (int)(maxProcessTime - (this.getEndAt() - begin));
                Util.sleep(gap);
                boolean willRollback = false;
                if (willRollback = isFailed()) {
                    rollback(order);
                } else {
                    t.exec();
                    this.setEndAt(Instant.now().getEpochSecond());
                }
                this.setEndAt(Instant.now().getEpochSecond());
                logger.debug("Processing @Step: " + this);
                return willRollback?FAILED:PROCESSING;
            }

            @Override
            public void rollback(Order order) {
                super.rollback(order);
                jedis.lrem(Storage.processingQueue, 1, JsonWriter.objectToJson(order));
                order.setCurrentStep(FAILED);
                order.getCurrentStep().setStartAt(0L);
                order.getCurrentStep().setEndAt(0L);
                jedis.lpush(Storage.preProcessingQueue, JsonWriter.objectToJson(order));
            }
        },

        PROCESSING {
            @Override
            public Step next(String originOrderInfo, Order order) {
                super.next(originOrderInfo, order);
                long begin = Instant.now().getEpochSecond();
                Transaction t = jedis.multi();
                t.lpush(Storage.postProcessingQueue, JsonWriter.objectToJson(order));
                t.lrem(Storage.processingQueue+ ":processing", 1, originOrderInfo);
                t.exec();

                this.setEndAt(Instant.now().getEpochSecond());
                int gap = (int)(maxProcessTime - (this.getEndAt() - begin));
                Util.sleep(gap);
                boolean willRollback = false;
                if (willRollback = isFailed()) {
                    rollback(order);
                } else {
                    t.exec();
                    this.setEndAt(Instant.now().getEpochSecond());
                }
                this.setEndAt(Instant.now().getEpochSecond());
                logger.debug("Processing @Step: " + this);
                return willRollback?FAILED: POST_PROCESSING;
            }

            @Override
            public void rollback(Order order) {
                super.rollback(order);
                jedis.lrem(Storage.postProcessingQueue, 1, JsonWriter.objectToJson(order));
                order.setCurrentStep(FAILED);
                order.getCurrentStep().setStartAt(0L);
                order.getCurrentStep().setEndAt(0L);
                jedis.lpush(Storage.processingQueue, JsonWriter.objectToJson(order));
            }
        },
        POST_PROCESSING {
            @Override
            public Step next(String originInfo, Order order) {
                super.next(originInfo, order);
                long begin = Instant.now().getEpochSecond();
                Transaction t = jedis.multi();
                t.lpush(Storage.completeQueue, JsonWriter.objectToJson(order));
                t.lrem(Storage.postProcessingQueue+ ":processing", 1, originInfo);
                t.exec();

                this.setEndAt(Instant.now().getEpochSecond());
                int gap = (int)(maxProcessTime - (this.getEndAt() - begin));
                Util.sleep(gap);
                boolean willRollback = false;
                if (willRollback = isFailed()) {
                    rollback(order);
                } else {
                    t.exec();
                    this.setEndAt(Instant.now().getEpochSecond());
                }
                this.setEndAt(Instant.now().getEpochSecond());
                logger.debug("Processing @Step: " + this);

                return willRollback?FAILED:COMPLETED;
            }

            @Override
            public void rollback(Order order) {
                super.rollback(order);
                jedis.lrem(Storage.completeQueue, 1, JsonWriter.objectToJson(order));
                order.setCurrentStep(FAILED);
                order.getCurrentStep().setStartAt(0L);
                order.getCurrentStep().setEndAt(0L);
                jedis.lpush(Storage.postProcessingQueue, JsonWriter.objectToJson(order));
            }
        },
        COMPLETED {},
        FAILED {};

        private long startAt = 0L;
        private long endAt = 0L;

        public Step next(String originOrderInfo, Order order) {
            order.setAcceptedAt(Instant.now().getEpochSecond());
            order.getCurrentStep().setStartAt(Instant.now().getEpochSecond());

            return this;
        }

        public void rollback(Order order) {logger.debug("rollback order: " + order);}

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
