package com.github.davidmoten.reels;

public enum Restart {
    DISPOSE_CHILDREN(true), LEAVE_CHILDREN(false);

    private final boolean disposeChildren;

    Restart(boolean disposeChildren) {
        this.disposeChildren = disposeChildren;
    }

    public boolean disposeChildren() {
        return disposeChildren;
    }

    @Override
    public String toString() {
        return "Restart";
    }
}
