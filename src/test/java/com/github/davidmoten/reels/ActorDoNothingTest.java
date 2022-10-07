package com.github.davidmoten.reels;

import org.junit.Test;

public class ActorDoNothingTest {

    @Test
    public void test() {
        ActorDoNothing.create().onMessage(null);
    }

}
