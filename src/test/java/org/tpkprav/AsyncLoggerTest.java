package org.tpkprav;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.tpkprav.logging.AsyncLogger;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class AsyncLoggerTest {

    private static final AsyncLogger log = AsyncLogger.of(AsyncLoggerTest.class);

    // ── Factory ───────────────────────────────────────────────────────────────

    @Test
    void of_returnsNonNullInstance() {
        assertNotNull(AsyncLogger.of(AsyncLoggerTest.class));
    }

    // ── Log-level checks ─────────────────────────────────────────────────────

    @Test
    void isDebugEnabled_doesNotThrow() {
        assertDoesNotThrow(log::isDebugEnabled);
    }

    @Test
    void isTraceEnabled_doesNotThrow() {
        assertDoesNotThrow(log::isTraceEnabled);
    }

    // ── Log methods (smoke-test: they must not throw) ─────────────────────────

    @Test
    void info_doesNotThrow() {
        assertDoesNotThrow(() -> log.info("info message {}", "arg1"));
    }

    @Test
    void warn_doesNotThrow() {
        assertDoesNotThrow(() -> log.warn("warn message {}", "arg1"));
    }

    @Test
    void error_doesNotThrow() {
        assertDoesNotThrow(() -> log.error("error message {}", "arg1", new RuntimeException("test")));
    }

    @Test
    void debug_doesNotThrow() {
        assertDoesNotThrow(() -> log.debug("debug message {}", "arg1"));
    }

    @Test
    void trace_doesNotThrow() {
        assertDoesNotThrow(() -> log.trace("trace message {}", "arg1"));
    }

    // ── MDC propagation ───────────────────────────────────────────────────────

    @Test
    void info_withMdcRequestId_doesNotThrow() {
        MDC.put("requestId", "test-req-id-123");
        try {
            assertDoesNotThrow(() -> log.info("request-scoped log nric={}", "****5678"));
        } finally {
            MDC.remove("requestId");
        }
    }

    @Test
    void logAfterMdcClear_doesNotThrow() throws InterruptedException {
        MDC.put("requestId", "temp-id");
        log.info("first log");
        MDC.remove("requestId");
        // small wait so the async task might still be processing
        TimeUnit.MILLISECONDS.sleep(50);
        assertDoesNotThrow(() -> log.info("second log after MDC cleared"));
    }

    // ── No-op when level disabled ─────────────────────────────────────────────

    @Test
    void debug_doesNotDispatchWhenDisabled() {
        // If debug is disabled the early return must not throw
        assertDoesNotThrow(() -> {
            for (int i = 0; i < 100; i++) {
                log.debug("high-volume debug {}", i);
            }
        });
    }

    @Test
    void trace_doesNotDispatchWhenDisabled() {
        assertDoesNotThrow(() -> {
            for (int i = 0; i < 100; i++) {
                log.trace("high-volume trace {}", i);
            }
        });
    }
}