package com.tramchester.graph.databaseManagement;

import org.neo4j.logging.Log;
import org.neo4j.logging.LogProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SLF4JLogProvider implements LogProvider {
    @Override
    public Log getLog(Class<?> loggingClass) {
        Logger logger = LoggerFactory.getLogger(loggingClass);
        return new WrapLogSLF4JLogger(logger);
    }

    @Override
    public Log getLog(String name) {
        Logger logger = LoggerFactory.getLogger(name);
        return new WrapLogSLF4JLogger(logger);
    }

    private static class WrapLogSLF4JLogger implements Log {
        private final Logger logger;

        public WrapLogSLF4JLogger(Logger logger) {
            this.logger = logger;
        }

        @Override
        public boolean isDebugEnabled() {
            return logger.isDebugEnabled();
        }

        @Override
        public void debug(String message) {
            logger.debug(message);
        }

        @Override
        public void debug(String message, Throwable throwable) {
            logger.debug(message, throwable);
        }

        @Override
        public void debug(String format, Object... arguments) {
            final String formatted = String.format(format, arguments);
            logger.debug(formatted);
        }

        @Override
        public void info(String message) {
            logger.info(message);
        }

        @Override
        public void info(String message, Throwable throwable) {
            logger.info(message, throwable);
        }

        @Override
        public void info(String format, Object... arguments) {
            final String formatted = String.format(format, arguments);
            logger.info(formatted);
        }

        @Override
        public void warn(String message) {
            logger.warn(message);
        }

        @Override
        public void warn(String message, Throwable throwable) {
            logger.warn(message, throwable);
        }

        @Override
        public void warn(String format, Object... arguments) {
            final String formatted = String.format(format, arguments);
            logger.warn(formatted);
        }

        @Override
        public void error(String message) {
            logger.error(message);
        }

        @Override
        public void error(String message, Throwable throwable) {
            logger.error(message, throwable);
        }

        @Override
        public void error(String format, Object... arguments) {
            final String formatted = String.format(format, arguments);
            logger.error(formatted);
        }
    }
}
