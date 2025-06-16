package edu.kit.kastel.vads.compiler.parser.ast.expression.operation;

import edu.kit.kastel.vads.compiler.lexer.tokens.Operator;
import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.parser.ast.expression.ExpressionTree;
import edu.kit.kastel.vads.compiler.parser.visitor.Visitor;
import java.util.Objects;

public final class BinaryOperationTree implements ExpressionTree {
    private ExpressionTree lhs;
    private ExpressionTree rhs;
    private final Operator.OperatorType operatorType;

    public BinaryOperationTree(
        ExpressionTree lhs, ExpressionTree rhs, Operator.OperatorType operatorType
    ) {
        this.lhs = lhs;
        this.rhs = rhs;
        this.operatorType = operatorType;
    }

    @Override
    public Span span() {
        return lhs().span().merge(rhs().span());
    }

    @Override
    public <T, R> R accept(Visitor<T, R> visitor, T data) {
        return visitor.visit(this, data);
    }

    public ExpressionTree lhs() {
        return lhs;
    }

    public void setLhs(ExpressionTree lhs) {
        this.lhs = lhs;
    }

    public ExpressionTree rhs() {
        return rhs;
    }

    public void setRhs(ExpressionTree rhs) {
        this.rhs = rhs;
    }

    public Operator.OperatorType operatorType() {
        return operatorType;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (BinaryOperationTree) obj;
        return Objects.equals(this.lhs, that.lhs) &&
            Objects.equals(this.rhs, that.rhs) &&
            Objects.equals(this.operatorType, that.operatorType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lhs, rhs, operatorType);
    }

    @Override
    public String toString() {
        return "BinaryOperationTree[" +
            "lhs=" + lhs + ", " +
            "rhs=" + rhs + ", " +
            "operatorType=" + operatorType + ']';
    }

}
