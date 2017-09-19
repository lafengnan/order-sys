package com.ebay.chris;

import com.ebay.chris.client.Client;
import com.ebay.chris.server.Server;
import org.apache.log4j.Logger;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

public class Main {
    private static final Logger logger = Logger.getLogger(Main.class);

    @Option(name = "-m", aliases = "-mode", usage = "startup as server or client mode")
    private String mode = "";

    public static void main(String... args) {
        Main main = new Main();
        parseArgs(main, args);
    }

    private static void parseArgs(Main bean, String... args) {
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
                Server server = new Server("demo");
                server.init();
                server.run();
            }

            if (bean.mode.equals("client")) {
                Client client = new Client();
                client.run();
            }

        } catch (CmdLineException e) {
            logger.debug(e.getMessage());
        }
    }
}
