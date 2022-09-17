package com.github.davidmoten.reels;

public final class DisposedException extends RuntimeException {

    private static final long serialVersionUID = -5619551616971144560L;

    public DisposedException(String message) {
        super (message);
    }
    
}
