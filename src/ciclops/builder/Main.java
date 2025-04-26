package ciclops.builder;

import common.logger.LogLevel;
import common.logger.Logger;

public class Main {
    public static void main(String[] args) {
        Logger.setMaxLogLevel(LogLevel.INFO);
        new Pipeline().run();
    }
}