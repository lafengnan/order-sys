package com.ebay.chris.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.apache.log4j.Logger;

public class BlueClientHandler extends SimpleChannelInboundHandler<String> {
    private static final Logger logger = Logger.getLogger(BlueClientHandler.class);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        logger.info(msg);
    }
}
