package com.ebay.chris.common;

import com.cedarsoftware.util.io.JsonReader;
import com.cedarsoftware.util.io.JsonWriter;
import com.ebay.chris.model.Order;
import org.apache.log4j.Logger;
import redis.clients.jedis.Jedis;

import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;


/**
 * Protocol defines the communication details between
 * client and server. In the system there are two kinds
 * of roles, client and server. To make them coordinate
 * with each other smoothly, we need to provide them with
 * a simple protocol. This protocol is simple and only be
 * used for demonstrate the communications in the system.
 *
 */
public class Protocol {
    private static String version = "1.0.0";
    private static final Jedis jedis = Util.jedis();
    private static Logger logger = Logger.getLogger(Protocol.class);

    public static <T> String send(Message<T> message) {
        logger.debug("Protocol version: " + version + ", sending message: " + message);
        // 1. validate message
        if (!validation(message)) {
            return MState.SEND_FAILED.toString();
        }

        // 2. send
        message.sendAt = Instant.now().getEpochSecond();
        switch (message.type) {
            case SUBMIT:
                final int timeOut = 60;
                Order order = (Order)message.body;
                jedis.lpush(Storage.stageQueue, JsonWriter.objectToJson(order));
                String[] orderIds = {""};

                // 2. query order id
                for (int i = 0; orderIds[0].isEmpty() && i < timeOut; ) {
                    List<String> infoList = jedis.lrange(Storage.stageQueue, 0, -1);
                    infoList.forEach(s -> {
                        Order updatedOrder = (Order)JsonReader.jsonToJava(s);
                        if (updatedOrder.getqId().equals(order.getqId())) {
                            orderIds[0] = updatedOrder.getId();
                        }
                    });
                    if (orderIds[0].isEmpty()) {
                        i += 5;
                        Util.sleep(5);
                    }
                }
                return orderIds[0];
            case QUERY:
                String orderId = (String)message.body;
                List<String> orderStrings = jedis.lrange(Storage.stageQueue, 0, -1);
                List<Order> orders = new LinkedList<>();
                orderStrings.forEach(s -> orders.add((Order)JsonReader.jsonToJava(s)));
                Optional<Order> opt = orders.stream().filter(o -> o.getId().equals(orderId)).findFirst();
                return opt.isPresent()?opt.get().toString():"null";
            case CHECK0:
                return Health.GREEN.toString();
            case CHECK1:
                return Health.GREEN.toString();
            default:
                return "";
        }
    }

    public static Health healthCheck() {
        // If all servers in backend are alive, returns green;
        // if major of servers are alive, returns yellow, otherwise
        // red is returned.

        return Health.GREEN;
    }

    public static <T> boolean validation(Message<T> message) {
        logger.debug("validation message format...");
        boolean flag = true;
        switch (version) {
            case "1.0.0":
                if (message.state.equals(MState.SEND_SUCCESS)) {
                    flag = false;
                    logger.debug("resend a sent message is forbidden!");
                }
                // go through higher version validation logic
            case "1.0.1":
                break;
            default:
                break;
        }
        logger.debug("validation completed, the format is " + (flag?"good":"bad"));
        return flag;
    }



    /** Message denotes a request/response message between client
     * and server communications.
     * Message body:
     * 1. order to be submitted
     * 2. orderId to be queried
     * 3. health state
     *    - GOOD
     *    - BAD
     */
    public static class Message <T> {
        // request id prefixed with 0
        // response id prefixed with 1
        private String id;
        private MType type;
        private String sender;
        private MState state;
        private long createdAt;
        private long sendAt;
        private long ackAt;
        private T body;

        public Message(String sender, MType type, T body) {
            // TODO: unique id
            String msgId = IdGenerator.messageId();
            this.id = (type.code & (1 << 4)) == 0?"#" + msgId:"*" + msgId;
            this.type = type;
            this.sender = sender;
            this.state = MState.INIT;
            this.createdAt = Instant.now().getEpochSecond();
            this.sendAt = 0L;
            this.ackAt = 0L;
            this.body = body;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public MType getType() {
            return type;
        }

        public void setType(MType type) {
            this.type = type;
        }

        public String getSender() {
            return sender;
        }

        public void setSender(String sender) {
            this.sender = sender;
        }

        public MState getState() {
            return state;
        }

        public void setState(MState state) {
            this.state = state;
        }

        public long getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(long createdAt) {
            this.createdAt = createdAt;
        }

        public long getSendAt() {
            return sendAt;
        }

        public void setSendAt(long sendAt) {
            this.sendAt = sendAt;
        }

        public long getAckAt() {
            return ackAt;
        }

        public void setAckAt(long ackAt) {
            this.ackAt = ackAt;
        }

        public T getBody() {
            return body;
        }

        public void setBody(T body) {
            this.body = body;
        }
    }

    /**
     * Message type defines four kinds of message that
     * could be send/received in system:
     * 1. CHECK0, used for health check in server cluster
     * 2. CHECK1, used for health check initialized by client
     * 3. QUERY, used for order info/status query
     * 4. SUBMIT, used for order submit
     * Kind 1 is dedicated to server role.
     * Kind 2, 3 and kind 4 are dedicated to client role.
     * code in [0 - 15] is reserved for server
     * code in [16 - 31] is reserved for client
     */
    public enum MType {
        CHECK0(0x00), // health check in servers
        CHECK1(0x10), // client check server health
        QUERY(0x11), // query orders, request only
        SUBMIT(0x12); // submit orders, request only
        private int code;
        private MType(int code) {
            this.code = code;
        }
    }

    /**
     * Message send state, each message has two states:
     * 1. SUCCESS, means the message has been sent successfully from source
     * 2. FAILED, means the message was sent with failures, needs retry or
     * discarding.
     */
    public enum MState {
        INIT,
        SEND_SUCCESS,
        SEND_FAILED;
    }

    public enum Health {
        GREEN(0),
        YELLOW(1),
        RED(2);
        private int value;
        private Health(int value) {
            this.value = value;
        }
    }
}
