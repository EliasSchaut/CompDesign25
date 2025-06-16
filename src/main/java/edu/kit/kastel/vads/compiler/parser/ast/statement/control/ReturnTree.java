package edu.kit.kastel.vads.compiler.parser.ast.statement.control;

import edu.kit.kastel.vads.compiler.Position;
import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.parser.ast.expression.ExpressionTree;
import edu.kit.kastel.vads.compiler.parser.visitor.Visitor;
import java.util.Objects;

public final class ReturnTree implements ControlTree {
    private ExpressionTree expression;
    private final Position start;

    public ReturnTree(ExpressionTree expression, Position start) {
        this.expression = expression;
        this.start = start;
    }

    @Override
    public Span span() {
        return new Span.SimpleSpan(start(), expression().span().end());
    }

    @Override
    public <T, R> R accept(Visitor<T, R> visitor, T data) {
        return visitor.visit(this, data);
    }

    public ExpressionTree expression() {
        return expression;
    }

    public void setExpression(ExpressionTree expression) {
        this.expression = expression;
    }

    public Position start() {
        return start;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (ReturnTree) obj;
        return Objects.equals(this.expression, that.expression) &&
            Objects.equals(this.start, that.start);
    }

    @Override
    public int hashCode() {
        return Objects.hash(expression, start);
    }

    @Override
    public String toString() {
        return "ReturnTree[" +
            "expression=" + expression + ", " +
            "start=" + start + ']';
    }

}
