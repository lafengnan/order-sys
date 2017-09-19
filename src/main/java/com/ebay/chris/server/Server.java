package com.ebay.chris.server;

import com.cedarsoftware.util.io.JsonReader;
import com.cedarsoftware.util.io.JsonWriter;
import com.ebay.chris.Runner;
import com.ebay.chris.common.IdGenerator;
import com.ebay.chris.common.Storage;
import com.ebay.chris.common.Util;
import com.ebay.chris.model.Order;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.apache.log4j.Logger;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import java.time.Instant;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Server {
    private static final Logger logger = Logger.getLogger(Server.class);
    private static final Jedis jedis = Util.jedis();

    @Setter @Getter
    private Engine engine;

    @Setter @Getter
    private MetaData info;
    private ThreadPoolExecutor pool;

    public Server(String name) {
        this.engine = new Engine();
        this.info = new MetaData();
        this.info.setName(name);
        this.info.setId(IdGenerator.serverId());
    }

    public void init() {
        logger.debug("initializing server...");
        // build thread pool
        buildThreadPool();
        // restore broken jobs in *tmp queues
    }

    public void close() {
        logger.debug("closing server...");
        // 1. clear unfinished jobs staged in *tmp queues

        // 2. close thread pool
        if (!pool.isShutdown()) {
            pool.shutdown();
        }

        // 3. exit
        System.exit(1);
    }

    public void run() {
        logger.debug("running server: " + info.getId());
        for (;;) {
            String orderInfo = pull();
            if (orderInfo == null || orderInfo.isEmpty()) {
                continue;
            }

            Order order = (Order)JsonReader.jsonToJava(orderInfo);
            if (order.getAcceptedAt() > 0L) {
                logger.debug("order: " + order.getId() +
                        " has been accepted at: " + order.getAcceptedAt());
                Util.sleep(5);
                continue;
            }

            // step 0: set orderId and push back to stage queue
            if (order.getId().isEmpty()) {
                order.setId(IdGenerator.orderId());
                order.setCurrentStep(Engine.Step.SUBMITTED);
                order.setAcceptedAt(Instant.now().getEpochSecond());

                Transaction t = jedis.multi();
                t.lpush(Storage.schedulingQueue, JsonWriter.objectToJson(order));
                t.lrem(Storage.stageQueue + ":in-progress", 1, orderInfo);
                t.exec();
            }
        }
    }

    private String pull() {
        logger.debug("pull order from redis, timeout 5s");
        return jedis.brpoplpush(Storage.stageQueue, Storage.stageQueue + ":in-progress", 5);
    }

    private void buildThreadPool() {
        pool = new ThreadPoolExecutor(
                5,
                10,
                60,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>());
    }

    private void process() {
        pool.getQueue().add(new Runnable() {
            @Override
            public void run() {
                // step1: scheduling
                // step2: pre-processing
                // step3: processing
                // step4: post-processing
            }
        });
    }
}

@Data
class MetaData {
    private String id;
    private String name;
}
