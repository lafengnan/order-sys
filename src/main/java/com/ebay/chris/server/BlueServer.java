package com.ebay.chris.server;

import com.ebay.chris.common.IdGenerator;
import com.ebay.chris.common.Util;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.Getter;
import lombok.Setter;
import org.apache.log4j.Logger;

import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BlueServer {
    private static final Logger logger = Logger.getLogger(BlueServer.class);
    @Getter
    private final long id = IdGenerator.serverId();
    @Getter
    private int port;
    @Getter @Setter
    private long ts;

    public BlueServer(int port) {
        this.port = port;
        this.ts = Instant.now().getEpochSecond();
    }

    public void run() throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup(); // (1)
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap(); // (2)
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class) // (3)
                    .childHandler(new BlueServerInitializer())  //(4)
                    .option(ChannelOption.SO_BACKLOG, 128)          // (5)
                    .childOption(ChannelOption.SO_KEEPALIVE, true); // (6)

            logger.debug("BlueServer is running" + ", port: " + this.port);
            // registry server into cluster
            Proxy.register(this);

            // bind port
            ChannelFuture f = b.bind(port).sync(); // (7)
            f.channel().closeFuture().sync();

        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
            logger.debug("BlueServer closed");
        }
    }

    public static void runProcessor() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                // waiting for server bootstrap
                Util.sleep(5);
                Engine engine = new Engine();
                engine.process();
            }
        }).start();
    }

    public static void start(int port) throws Exception {
        BlueServer server = new BlueServer(port);
        server.run();
    }
}
