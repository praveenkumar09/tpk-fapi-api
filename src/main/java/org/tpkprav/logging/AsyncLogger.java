package org.tpkprav.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.event.Level;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public final class AsyncLogger {

    private static final String MDC_REQUEST_ID = "requestId";

    private static final ExecutorService EXECUTOR;

    static {
        AtomicLong seq = new AtomicLong();
        EXECUTOR = Executors.newSingleThreadExecutor(r ->
                Thread.ofVirtual()
                        .name("async-log-" + seq.incrementAndGet())
                        .unstarted(r));
        Runtime.getRuntime().addShutdownHook(new Thread(AsyncLogger::flushAndShutdown, "async-log-shutdown"));
    }

    private final Logger delegate;

    private AsyncLogger(Logger delegate) {
        this.delegate = delegate;
    }

    public static AsyncLogger of(Class<?> clazz) {
        return new AsyncLogger(LoggerFactory.getLogger(clazz));
    }

    public boolean isDebugEnabled() {
        return delegate.isDebugEnabled();
    }

    public boolean isTraceEnabled() {
        return delegate.isTraceEnabled();
    }

    public void info(String pattern, Object... args) {
        dispatch(Level.INFO, pattern, args);
    }

    public void warn(String pattern, Object... args) {
        dispatch(Level.WARN, pattern, args);
    }

    public void error(String pattern, Object... args) {
        dispatch(Level.ERROR, pattern, args);
    }

    public void debug(String pattern, Object... args) {
        if (!delegate.isDebugEnabled()) {
            return;
        }
        dispatch(Level.DEBUG, pattern, args);
    }

    public void trace(String pattern, Object... args) {
        if (!delegate.isTraceEnabled()) {
            return;
        }
        dispatch(Level.TRACE, pattern, args);
    }

    private void dispatch(Level level, String pattern, Object[] args) {
        if (!isEnabled(level)) {
            return;
        }
        String requestId = MDC.get(MDC_REQUEST_ID);
        Runnable task = () -> emit(level, pattern, args, requestId);
        try {
            EXECUTOR.execute(task);
        } catch (RejectedExecutionException rejected) {
            task.run();
        }
    }

    private void emit(Level level, String pattern, Object[] args, String requestId) {
        String previous = MDC.get(MDC_REQUEST_ID);
        if (requestId != null) {
            MDC.put(MDC_REQUEST_ID, requestId);
        } else {
            MDC.remove(MDC_REQUEST_ID);
        }
        try {
            switch (level) {
                case TRACE -> delegate.trace(pattern, args);
                case DEBUG -> delegate.debug(pattern, args);
                case INFO  -> delegate.info(pattern, args);
                case WARN  -> delegate.warn(pattern, args);
                case ERROR -> delegate.error(pattern, args);
            }
        } finally {
            if (previous != null) {
                MDC.put(MDC_REQUEST_ID, previous);
            } else {
                MDC.remove(MDC_REQUEST_ID);
            }
        }
    }

    private boolean isEnabled(Level level) {
        return switch (level) {
            case TRACE -> delegate.isTraceEnabled();
            case DEBUG -> delegate.isDebugEnabled();
            case INFO  -> delegate.isInfoEnabled();
            case WARN  -> delegate.isWarnEnabled();
            case ERROR -> delegate.isErrorEnabled();
        };
    }

    private static void flushAndShutdown() {
        EXECUTOR.shutdown();
        try {
            if (!EXECUTOR.awaitTermination(2, TimeUnit.SECONDS)) {
                EXECUTOR.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            EXECUTOR.shutdownNow();
        }
    }
}
