package com.github.davidmoten.reels.internal;

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.davidmoten.reels.ActorRef;

public final class Heirarchy {

    private final Map<ActorRef<?>, ActorRef<?>> parents = new HashMap<>();
    private final Map<ActorRef<?>, List<ActorRef<?>>> children = new HashMap<>();
    private final Set<ActorRef<?>> actors = new HashSet<>();

    public Heirarchy() {

    }

    public void add(ActorRef<?> actor) {
        synchronized (parents) {
            actors.add(actor);
            actor.parent().ifPresent(p -> addChildTo(actor, p));
        }
    }

    public void remove(ActorRef<?> actor) {
        synchronized (parents) {
            actors.remove(actor);
            ActorRef<?> p = parents.remove(actor);
            if (p != null) {
                children.get(p).remove(actor);
            }
        }
    }

    public void addChildTo(ActorRef<?> child, ActorRef<?> parent) {
        synchronized (parents) {
            parents.put(child, parent);
            List<ActorRef<?>> list = children.get(parent);
            if (list == null) {
                list = new ArrayList<>();
                children.put(parent, list);
            }
            list.add(child);
        }
    }

    public void stop(ActorRef<?> actor) {
        // use a stack not recursion to protect against
        // stack overflow
        Deque<ActorRef<?>> stack = new LinkedList<>();
        stack.push(actor);
        ActorRef<?> a;
        while ((a = stack.poll()) != null) {
            a.stop();
            List<ActorRef<?>> list;
            synchronized (parents) {
                list = new ArrayList<>(children.get(a));
            }
            if (list != null) {
                for (ActorRef<?> child : list) {
                    stack.offer(child);
                }
            }
        }
    }

    public void dispose(ActorRef<?> actor) {
        // use a stack not recursion to protect against
        // stack overflow
        Deque<ActorRef<?>> stack = new LinkedList<>();
        stack.push(actor);
        ActorRef<?> a;
        while ((a = stack.poll()) != null) {
            ((ActorRefImpl<?>) a).disposeThis();
            List<ActorRef<?>> list;
            synchronized (parents) {
                ActorRef<?> p = parents.remove(a);
                if (p != null) {
                    children.get(p).remove(a);
                }
                list = children.get(a);
            }
            if (list != null) {
                for (ActorRef<?> child : list) {
                    stack.offer(child);
                }
            }
        }
    }
}
