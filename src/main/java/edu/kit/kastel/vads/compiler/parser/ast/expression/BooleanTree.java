package edu.kit.kastel.vads.compiler.parser.ast.expression;

import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.parser.visitor.Visitor;

public record BooleanTree(boolean value, Span span) implements ExpressionTree {
    @Override
    public <T, R> R accept(Visitor<T, R> visitor, T data) {
        return visitor.visit(this, data);
    }
}
