package edu.kit.kastel.vads.compiler.semantic;

import edu.kit.kastel.vads.compiler.parser.symbol.Name;
import edu.kit.kastel.vads.compiler.parser.visitor.Context;
import java.util.List;
import java.util.function.Predicate;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BinaryOperator;

public final class Namespace<T> implements Context<Namespace<T>> {

    private final Map<Name, T> content;

    public Namespace() {
        this.content = new HashMap<>();
    }

    private Namespace(Map<Name, T> content) {
        this.content = new HashMap<>(content);
    }

    public void put(Name name, T value) {
        this.content.put(name, value);
    }

    public void put(Name name, T value, BinaryOperator<T> merger) {
        this.content.merge(name, value, merger);
    }

    public @Nullable T get(Name name) {
        return this.content.get(name);
    }

    public List<Name> getAllWhere(Predicate<T> predicate) {
        return this.content.entrySet().stream()
                .filter(entry -> predicate.test(entry.getValue()))
                .map(Map.Entry::getKey)
                .toList();
    }

    public List<Name> names() {
        return List.copyOf(this.content.keySet());
    }

    @Override
    public Namespace<T> copy() {
        return new Namespace<>(content);
    }
}
