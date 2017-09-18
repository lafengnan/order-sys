package com.ebay.chris.server;

import com.cedarsoftware.util.io.JsonReader;
import com.ebay.chris.common.IdGenerator;
import com.ebay.chris.common.Storage;
import com.ebay.chris.common.Util;
import com.ebay.chris.model.Order;
import org.apache.log4j.Logger;
import redis.clients.jedis.Jedis;

import java.time.Instant;

public class Engine {
    private static final int failedRate = 5;
    private static final Jedis jedis = Util.jedis();

    private static final Logger logger = Logger.getLogger(Engine.class);

    public void process(Order order) {
        Step step = order.getCurrentStep();
        while (!step.equals(Step.COMPLETED)) {
            logger.debug("processing order at step: " + step);
            step = step.next();
        }
    }

    public enum Step {
        // Fake Steps
        CREATED {
            @Override
            public Step next() {
                String orderInfo = jedis.rpoplpush(Storage.stageQueue, Storage.stageQueue + ":tmp");
                Order order = (Order) JsonReader.jsonToJava(orderInfo);

                order.setId(IdGenerator.orderId());
                order.setAcceptedAt(Instant.now().getEpochSecond());
                order.getCurrentStep().setStartAt(Instant.now().getEpochSecond());

                return SUBMITTED;
            }
        },
        SUBMITTED {

        },

        // Process Steps
        SCHEDULING,
        PRE_PROCESSING,
        PROCESSING,
        POSTPROCESSING,
        COMPLETED;
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
