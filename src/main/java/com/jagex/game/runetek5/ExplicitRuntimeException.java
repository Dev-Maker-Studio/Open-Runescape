package com.jagex.game.runetek5;

/**
 * 
 * @author Brainpower - 28/01/2023
 *
 */
@SuppressWarnings("serial")
public class ExplicitRuntimeException extends RuntimeException {

    public final String error;
    public final Throwable throwable;

    public ExplicitRuntimeException(Throwable throwable, String error) {
        this.error = error;
        this.throwable = throwable;
        initCause(throwable);
    }

}
