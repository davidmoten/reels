package com.github.davidmoten.reels;

public interface Disposable {

    static Disposable NOOP = new Disposable() {

        @Override
        public void dispose() {
            // no-op
        }

        @Override
        public boolean isDisposed() {
            return true;
        }
    };

    void dispose();
    
    boolean isDisposed();

}
