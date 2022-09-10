package com.github.davidmoten.reels;

public interface Disposable {

    static Disposable NOOP = new Disposable() {

        @Override
        public void dispose() {
            // no-op
        }
    };

    void dispose();

}
