package com.nhnacademy.recommendation.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;

import java.util.function.Supplier;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TimingLog {

    public static Timer start(Logger log, String label) {
        return new Timer(log, label);
    }

    public static <T> T measure(Logger log, String label, Supplier<T> supplier) {
        try (Timer ignored = start(log, label)) {
            return supplier.get();
        }
    }

    public static void run(Logger log, String label, Runnable runnable) {
        try (Timer ignored = start(log, label)) {
            runnable.run();
        }
    }

    public static final class Timer implements AutoCloseable {
        private final Logger log;
        private final String label;
        private final long startNanos;

        private Timer(Logger log, String label) {
            this.log = log;
            this.label = label;
            this.startNanos = System.nanoTime();
        }

        @Override
        public void close() {
            log.info("{} elapsed={}ms", label, (System.nanoTime() - startNanos) / 1_000_000);
        }
    }
}
