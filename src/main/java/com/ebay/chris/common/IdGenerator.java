package com.ebay.chris.common;

import org.apache.log4j.Logger;
import redis.clients.jedis.Jedis;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

/**
 * Generate unique id for the system. The prototype of id generator
 * is implemented based on redis INCR command and UUID schema. In the
 * system, client id is a UUID and server id is a integer.
 */
public class IdGenerator {
    private static Logger logger = Logger.getLogger(IdGenerator.class);
    volatile private static Jedis jedis = Util.jedis();

    // redis key space
    private static final String merchantId = "999";
    private static final String orderIdKey = "str:order:orderId";
    private static final String serverIdKey = "str:order:serverId";
    private static final String msgIdKey = "str:order:msgId";

    public static String clientId() {
        return UUID.randomUUID().toString();
    }

    public static String serverId() {
        return jedis.incr(serverIdKey).toString();
    }

    public static String messageId() {
        return jedis.incr(msgIdKey).toString();
    }

    /**
     * OrderId consists of three parts:
     * 1. date in yyyymmdd format
     * 2. merchant id, here only 999 for demo
     * 3. counter in 6 digits, for instance 000001
     * In summary, one order id looks like:
     * 20170917999000001
     * @return orderId
     */
    public static String orderId() {
        StringBuilder builder = new StringBuilder();
        builder.append(today())
                .append(merchantId)
                .append(jedis.incr(orderIdKey));
        return builder.toString();
    }

    private static String today() {
        DateFormat format = new SimpleDateFormat("yyyyMMdd");
        return format.format(new Date());
    }

}
