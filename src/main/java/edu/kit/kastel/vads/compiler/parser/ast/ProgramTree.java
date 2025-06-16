package edu.kit.kastel.vads.compiler.parser.ast;

import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.parser.visitor.Visitor;

import java.util.ArrayList;
import java.util.List;

public record ProgramTree(List<FunctionTree> topLevelTrees) implements Tree {
    public ProgramTree {
        assert !topLevelTrees.isEmpty() : "must be non-empty";
        topLevelTrees = new ArrayList<>(topLevelTrees);
    }

    @Override
    public Span span() {
        var first = topLevelTrees.getFirst();
        var last = topLevelTrees.getLast();
        return new Span.SimpleSpan(first.span().start(), last.span().end());
    }

    @Override
    public <T, R> R accept(Visitor<T, R> visitor, T data) {
        return visitor.visit(this, data);
    }

    public void addFunction(FunctionTree function) {
        topLevelTrees.add(function);
    }

    public void removeFunction(FunctionTree function) {
        topLevelTrees.remove(function);
    }

    public void setFunction(int index, FunctionTree function) {
        if (index < 0 || index >= topLevelTrees.size()) {
            throw new IndexOutOfBoundsException(
                "Index: " + index + ", Size: " + topLevelTrees.size());
        }
        topLevelTrees.set(index, function);
    }
}
