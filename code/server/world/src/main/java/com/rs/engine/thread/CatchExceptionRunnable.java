package com.rs.engine.thread;

import org.darkan.core.Logger;

/**
 * A Runnable wrapper that catches and logs exceptions instead of letting them propagate.
 */
public class CatchExceptionRunnable implements Runnable {
    private final Runnable delegate;

    public CatchExceptionRunnable(Runnable delegate) {
        this.delegate = delegate;
    }

    @Override
    public void run() {
        try {
            delegate.run();
        } catch (Throwable e) {
            Logger.handle(CatchExceptionRunnable.class, "run", e);
        }
    }
}
