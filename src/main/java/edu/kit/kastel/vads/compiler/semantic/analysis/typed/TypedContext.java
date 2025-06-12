package edu.kit.kastel.vads.compiler.semantic.analysis.typed;

import edu.kit.kastel.vads.compiler.parser.symbol.Name;
import edu.kit.kastel.vads.compiler.parser.type.Type;
import edu.kit.kastel.vads.compiler.semantic.SemanticException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import org.jspecify.annotations.Nullable;

public final class TypedContext {
    public record Context(HashMap<Name, Type> variableTypes) {
        public Context(Context other) {
            this(new HashMap<>(other.variableTypes));
        }
    }

    private final Deque<Context> stack = new ArrayDeque<>();

    public TypedContext() {
        // Initialize with a global context
        stack.push(new Context(new HashMap<>()));
    }

    public void push() {
        if (stack.isEmpty()) {
            stack.push(new Context(new HashMap<>()));
        } else {
            var latestContext = stack.peek();
            stack.push(new Context(latestContext));
        }
    }

    public void pop() {
        if (stack.isEmpty()) {
            throw new IllegalStateException("No context available");
        }
        stack.pop();
    }

    public void addVariable(Name name, Type type) {
        var latestContext = stack.peek();
        if (latestContext == null) {
            throw new IllegalStateException("No context available");
        }

        if (latestContext.variableTypes.containsKey(name)) {
            throw new SemanticException("Variable " + name + " is already declared in context");
        }
        latestContext.variableTypes.put(name, type);
    }

    public Type getVariableType(Name name) {
        var type = tryGetVariableType(name);
        if (type == null) {
            throw new SemanticException("Variable " + name + " is not declared in context");
        }
        return type;
    }

    public @Nullable Type tryGetVariableType(Name name) {
        var latestContext = stack.peek();
        if (latestContext == null) {
            throw new IllegalStateException("No context available");
        }

        return latestContext.variableTypes.get(name);
    }
}
