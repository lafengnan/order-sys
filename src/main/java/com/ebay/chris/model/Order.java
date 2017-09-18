package com.ebay.chris.model;

import com.ebay.chris.server.Engine.*;

import java.util.UUID;


/**
 * Order class represents order model.
 */
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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(long submittedAt) {
        this.submittedAt = submittedAt;
    }

    public long getAcceptedAt() {
        return acceptedAt;
    }

    public void setAcceptedAt(long acceptedAt) {
        this.acceptedAt = acceptedAt;
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

    public Step getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(Step currentStep) {
        this.currentStep = currentStep;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public String getqId() {
        return qId;
    }

    public void setqId(String qId) {
        this.qId = qId;
    }

    @Override
    public String toString() {
        return "Order{" +
                "id='" + id + '\'' +
                ", userId='" + userId + '\'' +
                ", amount=" + amount +
                ", submittedAt=" + submittedAt +
                ", acceptedAt=" + acceptedAt +
                ", startAt=" + startAt +
                ", endAt=" + endAt +
                ", currentStep=" + currentStep +
                '}';
    }
}
