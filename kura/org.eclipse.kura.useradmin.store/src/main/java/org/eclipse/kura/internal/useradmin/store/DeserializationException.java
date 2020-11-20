package org.eclipse.kura.internal.useradmin.store;

class DeserializationException extends Exception {

    private static final long serialVersionUID = 2831107630794357637L;

    public DeserializationException(String message, Throwable cause) {
        super(message, cause);
    }

    public DeserializationException(String message) {
        super(message);
    }

    public DeserializationException(Throwable cause) {
        super(cause);
    }

}