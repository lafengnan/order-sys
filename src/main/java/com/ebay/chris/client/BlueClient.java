package com.ebay.chris.client;

import com.ebay.chris.server.OrderCore;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.Getter;
import lombok.Setter;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class BlueClient {
    private static final Logger logger = Logger.getLogger(BlueClient.class);

    @Setter @Getter
    private String serverHost;
    @Setter @Getter
    private int serverPort;
    private OrderCore core;

    public BlueClient(final String serverHost, final int serverPort) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.core = new OrderCore();
    }

    public BlueClient() {
        this.core = new OrderCore();
    }

    public void run() throws Exception {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap  = new Bootstrap()
                    .group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new BlueClientInitializer());
            Channel channel = bootstrap.connect(serverHost, serverPort).sync().channel();
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            while(true){
                System.out.printf("$> ");
                channel.writeAndFlush(in.readLine() + "\r\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            group.shutdownGracefully();
        }
    }

    public void once(String msg) throws Exception {
         EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap  = new Bootstrap()
                    .group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new BlueClientInitializer());
            Channel channel = bootstrap.connect(serverHost, serverPort).sync().channel();
                channel.writeAndFlush(msg + "\r\n");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            group.shutdownGracefully();
        }
    }
}
