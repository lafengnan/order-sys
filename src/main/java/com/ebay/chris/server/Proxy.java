package com.ebay.chris.server;

import com.cedarsoftware.util.io.JsonReader;
import com.cedarsoftware.util.io.JsonWriter;
import com.ebay.chris.client.BlueClient;
import com.ebay.chris.common.Util;
import org.apache.log4j.Logger;
import redis.clients.jedis.Jedis;

import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * Proxy consists of several BlueServer instance.
 *
 */
public class Proxy extends Thread{
    private static final Logger logger = Logger.getLogger(Proxy.class);
    private static List<BlueServer> servers = new LinkedList<>();
    private static final String cluster = "list:order:cluster";
    private static final Jedis jedis = Util.jedis();
    private static final int DEAD_THROTTLE = 5;

    public static void load() {
        if (!servers.isEmpty()) return;
        logger.info(jedis.echo("blue"));
        if (jedis.llen(cluster) > 0) {
            List<String> info = jedis.lrange(cluster, 0, -1);
            info.forEach(s -> {
                servers.add((BlueServer) JsonReader.jsonToJava(s));
            });
        }
    }

    public static BlueServer pickUp() {
        Random random = new Random();
        return servers.get(random.nextInt(jedis.llen(cluster).intValue()));
    }

    public static List<BlueServer> register(BlueServer server) {
        jedis.lpush(cluster, JsonWriter.objectToJson(server));
        return servers;
    }

    public static List<BlueServer> remove(long id) {
        List<String> info = jedis.lrange(cluster, 0, -1);
        List<BlueServer> servers = new LinkedList<>();
        info.forEach(s->servers.add((BlueServer)JsonReader.jsonToJava(s)));
        servers.stream()
                .filter(server -> server.getId() == id)
                .forEach(server -> jedis.lrem(cluster, 1, JsonWriter.objectToJson(server)));
        servers.removeIf(server -> server.getId() == id);
        return servers;
    }

    /**
     * Clean server info from register if the server has not
     * update timestamp for more than 5s.
     */
    public static void clean() {
        for (;;) {
            load();
            long now = Instant.now().getEpochSecond();
            servers.forEach(server -> {
                if (now - server.getTs() > DEAD_THROTTLE) {
                    jedis.lrem(cluster, 1, JsonWriter.objectToJson(server));
                }
            });
        }
    }
}
