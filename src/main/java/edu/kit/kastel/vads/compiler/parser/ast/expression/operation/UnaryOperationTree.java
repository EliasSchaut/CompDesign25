package edu.kit.kastel.vads.compiler.parser.ast.expression.operation;

import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.lexer.tokens.Operator;
import edu.kit.kastel.vads.compiler.parser.ast.expression.ExpressionTree;
import edu.kit.kastel.vads.compiler.parser.visitor.Visitor;
import java.util.Objects;

public final class UnaryOperationTree implements ExpressionTree {
    private ExpressionTree operand;
    private final Operator operator;

    public UnaryOperationTree(
        ExpressionTree operand,
        Operator operator
    ) {
        this.operand = operand;
        this.operator = operator;
    }

    @Override
    public Span span() {
        return operator.span().merge(operand.span());
    }

    @Override
    public <T, R> R accept(Visitor<T, R> visitor, T data) {
        return visitor.visit(this, data);
    }

    public ExpressionTree operand() {
        return operand;
    }

    public void setOperand(ExpressionTree operand) {
        this.operand = operand;
    }

    public Operator operator() {
        return operator;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (UnaryOperationTree) obj;
        return Objects.equals(this.operand, that.operand) &&
            Objects.equals(this.operator, that.operator);
    }

    @Override
    public int hashCode() {
        return Objects.hash(operand, operator);
    }

    @Override
    public String toString() {
        return "UnaryOperationTree[" +
            "operand=" + operand + ", " +
            "operator=" + operator + ']';
    }

}
