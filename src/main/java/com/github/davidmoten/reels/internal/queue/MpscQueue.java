package com.github.davidmoten.reels.internal.queue;

import java.util.concurrent.atomic.AtomicReference;

public class MpscQueue<T> {

    private final AtomicReference<Node<T>> consumerNode;
    private final AtomicReference<Node<T>> producerNode;

    public MpscQueue() {
        Node<T> node = new Node<>();
        consumerNode = new AtomicReference<>(node);
        producerNode = new AtomicReference<>(node);
    }

    public boolean offer(T value) {
        Node<T> node = new Node<>();
        Node<T> prev = producerNode.getAndSet(node);
        prev.value = value;
        prev.setNext(node);
        return true;
    }

    public T poll() {
        Node<T> node = consumerNode.get();
        Node<T> next = node.getNext();
        T v = node.value;
        if (v != null) {
            if (next == null) {
                if (node != producerNode.get()) {
                    while ((next = node.getNext()) == null) {
                        //spin
                    }
                    v = node.value;
                    consumerNode.lazySet(next);
                } else {
                    node.value = null;
                }
            } else {
                consumerNode.lazySet(next);
            }
        }
        return v;
    }

    static final class Node<T> extends AtomicReference<Node<T>> {
        private static final long serialVersionUID = 2232102237291327527L;
        T value;

        Node() {
        }

        void setNext(Node<T> node) {
            lazySet(node);
        }

        Node<T> getNext() {
            return get();
        }

    }

}
