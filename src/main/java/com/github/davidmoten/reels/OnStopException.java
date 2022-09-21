package com.github.davidmoten.reels;

public final class OnStopException extends RuntimeException {

    private static final long serialVersionUID = -8758473112596135828L;

    public OnStopException(Throwable e) {
        super(e);
    }
}
