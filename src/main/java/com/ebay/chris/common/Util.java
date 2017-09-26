package com.ebay.chris.common;

import org.apache.log4j.Logger;
import redis.clients.jedis.Jedis;

public class Util {
    private static Logger logger = Logger.getLogger(Util.class);
    private volatile static Jedis jedis;
    private static final int defaultPort = 6379;
    private static final String defaultHost = "127.0.0.1";

    // singleton for demo
    private Util() {
    }

    public static Jedis jedis() {
        if (jedis == null) {
            synchronized (IdGenerator.class) {
                if (jedis == null) {
                    logger.debug("build jedis connection...");
                    jedis = new Jedis(defaultHost, defaultPort);
                }
            }
        }
        return jedis;
    }

    public static void sleep(int seconds) {
        try {
            logger.debug("sleep " + seconds + "s");
            Thread.sleep(seconds*1000);
        } catch (InterruptedException e) {
            logger.debug("sleep interrupted!");
        }
    }
}
