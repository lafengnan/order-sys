package com.ebay.chris.client;

import com.cedarsoftware.util.io.JsonReader;
import com.cedarsoftware.util.io.JsonWriter;
import com.ebay.chris.Runner;
import com.ebay.chris.common.IdGenerator;
import com.ebay.chris.common.Protocol.*;
import com.ebay.chris.common.Storage;
import com.ebay.chris.common.Util;
import com.ebay.chris.model.Order;
import org.apache.log4j.Logger;

import java.time.Instant;
import java.util.*;

/**
 * This project is implemented as a CLI app, for convenience of
 * demonstration, client is used to input user requests. To simplify
 * the prototype, the communication between client and server is supported
 * by using message queue based on redis. That means client and server will
 * not talk with each other directly. They are decoupled by MQ.
 */
public class Client extends Runner {
    private static Logger logger = Logger.getLogger(Client.class);
    private static final Map<String, Object> JSONWR_WARGS = new HashMap<>();

    static {
        JSONWR_WARGS.put(JsonWriter.PRETTY_PRINT, true);
        JSONWR_WARGS.put(JsonWriter.DATE_FORMAT, "yyyyMMdd");
    }

    // clientId identifies client uniquely
    private String id = IdGenerator.clientId();

    public Client() {
    }

    public void run() {
        for (;;) {
            System.out.printf(id + "@client> ");
            Scanner in = new Scanner(System.in);
            if (in.hasNext()) {
                String cmd = in.next();
                switch (cmd) {
                    case "query":
                        String orderId = in.next();
                        String info = query(orderId);
                        System.out.println(info);
                        break;
                    case "submit":
                        String userId = in.next();
                        int amount = in.nextInt();
                        System.out.println(submit(createOrder(userId, amount)));
                        break;
                    case "check":
                        break;
                    default:
                        logger.warn("Unknown command: " + cmd);
                        break;
                }
            }
        }
    }

    private Order createOrder(String userId, int amount) {
        Order order = new Order();
        order.setUserId(userId);
        order.setAmount(amount);
        order.setCreatedAt(Instant.now().getEpochSecond());

        return order;
    }
    /**
     * Submit a new CREATED order to server. The orders to submit SHOULD be in
     * CREATED state, any other states are forbidden to submit.
     * @param order new created order
     * @return orderId to client
     */
    private String submit(Order order) {
        logger.debug("Submit order to service...");
        Message<Order> message = new Message<>(id, MType.SUBMIT, order);
        return execute(Command.SUBMIT, message);
    }

    private String query(String orderId) {
        logger.debug("Query order with id: " + orderId);
        Message<String> message = new Message<>(id, MType.QUERY, orderId);
        return execute(Command.QUERY, message);
    }

    private <T> String execute(Command cmd, Message<T> message) {
        logger.debug("Sending message from client: " + id);
        return cmd.execute(message);
    }
}
