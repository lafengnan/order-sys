package com.ebay.chris.server;

import com.cedarsoftware.util.io.JsonWriter;
import com.ebay.chris.client.Command;
import com.ebay.chris.common.IdGenerator;
import com.ebay.chris.common.Protocol.*;
import com.ebay.chris.model.Order;
import org.apache.log4j.Logger;

import java.time.Instant;
import java.util.*;

public class OrderCore {
    private static Logger logger = Logger.getLogger(OrderCore.class);
    private static final Map<String, Object> JSONWR_WARGS = new HashMap<>();

    static {
        JSONWR_WARGS.put(JsonWriter.PRETTY_PRINT, true);
        JSONWR_WARGS.put(JsonWriter.DATE_FORMAT, "yyyyMMdd");
    }

    // clientId identifies client uniquely
    private String id = IdGenerator.clientId();

    public OrderCore() {
    }

    public String execute(String[] contents) {
        String resp = "";
        String cmd = contents[0];
        switch (cmd) {
            case "query":
                String orderId = contents[1];
                resp = query(orderId);
                break;
            case "submit":
                String userId = contents[1];
                int amount = Integer.valueOf(contents[2]);
                resp = submit(createOrder(userId, amount));
                break;
            case "check":
                break;
            default:
                resp = "unknown command: " + cmd;
                logger.warn(resp);
                break;
        }
        return resp;
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
        Message<Order> message = new Message<>(id, MType.SUBMIT, order);
        return execute(Command.SUBMIT, message);
    }

    private String query(String orderId) {
        Message<String> message = new Message<>(id, MType.QUERY, orderId);
        return execute(Command.QUERY, message);
    }

    private <T> String execute(Command cmd, Message<T> message) {
        return cmd.execute(message);
    }
}
