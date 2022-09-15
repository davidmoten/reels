package com.github.davidmoten.reels;

public final class CreateException extends RuntimeException {

    private static final long serialVersionUID = -5062542557166661510L;

    public CreateException(Throwable t) {
        super(t);
    }

    public CreateException(String message) {
        super(message);
    }

    public CreateException(String message, Throwable cause) {
        super(message, cause);
    }

}
