package com.github.davidmoten.reels;

public final class DisposedException extends RuntimeException {

    private static final long serialVersionUID = 5105716518981717774L;

    public DisposedException(String message) {
        super(message);
    }

}
