package com.github.davidmoten.reels;

import java.util.Optional;

public interface MessageContext<T> {

    ActorRef<T> self();

    <S> Optional<ActorRef<S>> sender();

    Context context();

}
