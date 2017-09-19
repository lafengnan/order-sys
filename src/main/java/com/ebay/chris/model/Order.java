package com.ebay.chris.model;

import com.ebay.chris.server.Engine.*;
import lombok.Data;

import java.util.UUID;


/**
 * Order class represents order model.
 */
@Data
public class Order {
    // order id
    private String id;
    // user id of order
    private String userId;
    // amount of one order
    private int amount;
    // query id from client
    private String qId;
    // the timestamp of order created at client
    private long createdAt;
    // the timestamp of order submitted from client
    private long submittedAt;
    // the timestamp of order accepted by server
    private long acceptedAt;
    // the timestamp of order initialized to be processed
    private long startAt;
    // the timestamp of order processing completed
    private long endAt;
    // current processing step of order
    private Step currentStep;

    public Order() {
        this.id = "";
        this.userId = "";
        this.amount = 0;
        this.qId = UUID.randomUUID().toString();
        this.submittedAt = 0L;
        this.acceptedAt = 0L;
        this.startAt = 0L;
        this.endAt = 0L;
        this.currentStep = Step.CREATED;
    }
}
