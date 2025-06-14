package edu.kit.kastel.vads.compiler.parser.visitor;

import java.util.ArrayDeque;
import java.util.Deque;


public final class ScopedContext<T extends Context<T>> {
    private final Deque<T> stack = new ArrayDeque<>();

    public ScopedContext(T data) {
        // Initialize with a global context
        stack.push(data);
    }

    public void push() {
        if (stack.isEmpty()) {
            throw new IllegalStateException("No context available");
        } else {
            var latestContext = stack.peek();
            stack.push(latestContext.copy());
        }
    }

    public void pop() {
        if (stack.isEmpty()) {
            throw new IllegalStateException("No context available");
        }
        stack.pop();
    }

    public T get() {
        if (stack.isEmpty()) {
            throw new IllegalStateException("No context available");
        }
        return stack.peek();
    }
}
