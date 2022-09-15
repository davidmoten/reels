package com.github.davidmoten.reels;

public interface Disposable {

    void dispose();
    
    boolean isDisposed();
    
    static Disposable DISPOSED = new Disposed();
    
    static final class Disposed implements Disposable {

        @Override
        public void dispose() {
            // do nothing
        }

        @Override
        public boolean isDisposed() {
            return true;
        }
    }

}
