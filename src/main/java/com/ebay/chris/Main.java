package com.ebay.chris;

import com.ebay.chris.client.BlueClient;
import com.ebay.chris.server.BlueServer;
import org.apache.log4j.Logger;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

public class Main {
    private static final Logger logger = Logger.getLogger(Main.class);

    @Option(name = "-m", aliases = "-mode", usage = "startup as server or client mode")
    private String mode = "";

    public static void main(String... args) throws Exception{
        Main main = new Main();
        parseArgs(main, args);
    }

    private static void parseArgs(Main bean, String... args) throws Exception {
        CmdLineParser parser = new CmdLineParser(bean);
        try {
            parser.parseArgument(args);
            if (bean.mode.isEmpty()
                    || (!bean.mode.equals("server")
                    && !bean.mode.equals("client"))) {
                logger.debug("Nonsupport mode: " + bean.mode);
                System.exit(-1);
            }

            if (bean.mode.equals("server")) {
                BlueServer.start(8090);
            }

            if (bean.mode.equals("client")) {
                new BlueClient("localhost", 8090).run();
            }

        } catch (CmdLineException e) {
            logger.debug(e.getMessage());
        }
    }
}
