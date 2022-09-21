package com.github.davidmoten.reels.internal;

import com.github.davidmoten.reels.Actor;
import com.github.davidmoten.reels.Message;
import com.github.davidmoten.reels.MessageContext;

public final class RootActor implements Actor<Object> {
    
    public RootActor() {
        
    }

    @Override
    public void onMessage(Message<Object> message) {
        // TODO
    }

    @Override
    public void onStop(MessageContext<Object> context) {
        // TODO Auto-generated method stub
        
    }
}