package edu.kit.kastel.vads.compiler.parser.ast.expression;

import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.parser.visitor.Visitor;

public record TernaryOperationTree(ExpressionTree condition, ExpressionTree trueBranch,
                                   ExpressionTree falseBranch)
    implements ExpressionTree {

    @Override
    public Span span() {
        return condition().span().merge(falseBranch().span());
    }

    @Override
    public <T, R> R accept(Visitor<T, R> visitor, T data) {
        return visitor.visit(this, data);
    }
}
