package edu.kit.kastel.vads.compiler.parser.ast.statement.control;

import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.parser.ast.expression.ExpressionTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.BlockTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.StatementTree;
import edu.kit.kastel.vads.compiler.parser.visitor.Visitor;
import org.jspecify.annotations.Nullable;

public record ForTree(@Nullable StatementTree init,
                      ExpressionTree condition,
                      @Nullable StatementTree update,
                      BlockTree body) implements ControlTree {
    @Override
    public Span span() {
        StatementTree initTree = init();
        if (initTree == null) {
            return condition.span().merge(body.span());
        } else {
            return init.span().merge(body.span());
        }
    }

    @Override
    public <T, R> R accept(Visitor<T, R> visitor, T data) {
        return visitor.visit(this, data);
    }
}
