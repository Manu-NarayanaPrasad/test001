package com.envelopes.apps.labelprinter;

/**
 * Created by Manu on 7/12/2016.
 */
public class LabelNotFoundException extends Exception {
    public LabelNotFoundException() {
        super();
    }

    public LabelNotFoundException(String message) {
        super(message);
    }

    public LabelNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public LabelNotFoundException(Throwable cause) {
        super(cause);
    }

    protected LabelNotFoundException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
