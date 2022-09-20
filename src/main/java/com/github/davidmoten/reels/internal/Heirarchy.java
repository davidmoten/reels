package com.github.davidmoten.reels.internal;

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.github.davidmoten.reels.ActorRef;
import com.github.davidmoten.reels.CreateException;

public final class Heirarchy {

    private final Map<ActorRef<?>, ActorRef<?>> parents = new HashMap<>();
    private final Map<ActorRef<?>, List<ActorRef<?>>> children = new HashMap<>();
    private final Map<String, ActorRef<?>> actors = new HashMap<>();
    private ActorRef<?> root;

    public Heirarchy() {
    }

    public void setRoot(ActorRef<?> root) {
        this.root = root;
    }

    public void add(ActorRef<?> actor) {
        synchronized (parents) {
            if (actors.put(actor.name(), actor) != null) {
                throw new CreateException("actor with that name already exists");
            }
            actor.parent().ifPresent(p -> addChildTo(actor, p));
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

    public boolean remove(ActorRef<?> actor) {
        synchronized (parents) {
            if (actors.remove(actor.name()) == null) {
                return false;
            }
            ActorRef<?> p = parents.remove(actor);
            if (p != null) {
                children.get(p).remove(actor);
            }
            return true;
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
                list = children.get(a);
                if (list != null) {
                    list = new ArrayList<>(list);
                }
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
                actors.remove(a.name(), a);
            }
            if (list != null) {
                for (ActorRef<?> child : list) {
                    stack.offer(child);
                }
            }
        }
    }

    public boolean isEmpty() {
        synchronized (parents) {
            return actors.isEmpty();
        }
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<ActorRef<T>> get(String name) {
        synchronized (parents) {
            return (Optional<ActorRef<T>>) (Optional<?>) Optional.ofNullable(actors.get(name));
        }
    }

    public void dispose() {
        dispose(root);
    }

    public void stop() {
        stop(root);
    }
}
