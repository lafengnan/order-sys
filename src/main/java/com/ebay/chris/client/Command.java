package com.ebay.chris.client;

import com.ebay.chris.common.Protocol;

/**
 * Command defines the commands client supported.
 * QUERY are used for order info query
 * SUBMIT are used to submit a new order
 * HEALTH_CHECK is a command to check server status
 * from client side
 */
public enum Command {
    QUERY,
    SUBMIT,
    HEALTH_CHECK;

    public <T> String execute(Protocol.Message<T> message) {
        switch (this) {
            case SUBMIT:
                message.setType(Protocol.MType.SUBMIT);
                break;
            case QUERY:
                message.setType(Protocol.MType.QUERY);
                break;
            case HEALTH_CHECK:
                message.setType(Protocol.MType.CHECK0);
                break;
            default:
                message.setType(Protocol.MType.UNKNOWN);
                break;
        }
        return Protocol.send(message);
    }
}
