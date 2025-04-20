package ciclops.builder;

import common.logger.LogLevel;
import common.logger.Logger;

public class Main {
    private static final Logger LOGGER = new Logger(Main.class);

    public static void main(String[] args) {
        Logger.setMaxLogLevel(LogLevel.DEBUG);
        new Pipeline().run();
    }
}