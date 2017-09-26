package com.ebay.chris.common;

public class Storage {
    public static final String stageQueue = "list:order:queue:stage";
    public static final String schedulingQueue = "list:order:queue:sch";
    public static final String preProcessingQueue = "list:order:queue:prep";
    public static final String processingQueue = "list:order:queue:proc";
    public static final String postProcessingQueue = "list:order:queue:post";
    public static final String completeQueue = "list:order:queue:completed";

    public static String[] queues = {
            stageQueue,
            stageQueue + ":processing",
            schedulingQueue,
            schedulingQueue + ":processing",
            preProcessingQueue,
            preProcessingQueue + ":processing",
            processingQueue,
            processingQueue + ":processing",
            postProcessingQueue,
            postProcessingQueue + ":processing",
            completeQueue};
}
