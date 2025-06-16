package edu.kit.kastel.vads.compiler.parser.ast.expression.operation;

import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.parser.ast.expression.ExpressionTree;
import edu.kit.kastel.vads.compiler.parser.visitor.Visitor;
import java.util.Objects;

public final class TernaryOperationTree
    implements ExpressionTree {
    private ExpressionTree condition;
    private ExpressionTree trueBranch;
    private ExpressionTree falseBranch;

    public TernaryOperationTree(ExpressionTree condition,
                                ExpressionTree trueBranch,
                                ExpressionTree falseBranch) {
        this.condition = condition;
        this.trueBranch = trueBranch;
        this.falseBranch = falseBranch;
    }

    @Override
    public Span span() {
        return condition().span().merge(falseBranch().span());
    }

    @Override
    public <T, R> R accept(Visitor<T, R> visitor, T data) {
        return visitor.visit(this, data);
    }

    public ExpressionTree condition() {
        return condition;
    }

    public void setCondition(ExpressionTree condition) {
        this.condition = condition;
    }

    public ExpressionTree trueBranch() {
        return trueBranch;
    }

    public void setTrueBranch(ExpressionTree trueBranch) {
        this.trueBranch = trueBranch;
    }

    public ExpressionTree falseBranch() {
        return falseBranch;
    }

    public void setFalseBranch(ExpressionTree falseBranch) {
        this.falseBranch = falseBranch;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (TernaryOperationTree) obj;
        return Objects.equals(this.condition, that.condition) &&
            Objects.equals(this.trueBranch, that.trueBranch) &&
            Objects.equals(this.falseBranch, that.falseBranch);
    }

    @Override
    public int hashCode() {
        return Objects.hash(condition, trueBranch, falseBranch);
    }

    @Override
    public String toString() {
        return "TernaryOperationTree[" +
            "condition=" + condition + ", " +
            "trueBranch=" + trueBranch + ", " +
            "falseBranch=" + falseBranch + ']';
    }

}
