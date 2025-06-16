package edu.kit.kastel.vads.compiler.parser.ast.statement;

import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.lexer.tokens.Operator;
import edu.kit.kastel.vads.compiler.parser.ast.expression.ExpressionTree;
import edu.kit.kastel.vads.compiler.parser.ast.lvalue.LValueTree;
import edu.kit.kastel.vads.compiler.parser.visitor.Visitor;
import java.util.Objects;

public final class AssignmentTree implements StatementTree {
    private LValueTree lValue;
    private final Operator operator;
    private ExpressionTree expression;

    public AssignmentTree(LValueTree lValue, Operator operator, ExpressionTree expression) {
        this.lValue = lValue;
        this.operator = operator;
        this.expression = expression;
    }

    @Override
    public Span span() {
        return lValue().span().merge(expression().span());
    }

    @Override
    public <T, R> R accept(Visitor<T, R> visitor, T data) {
        return visitor.visit(this, data);
    }

    public LValueTree lValue() {
        return lValue;
    }

    public void setLvalue(LValueTree lValue) {
        this.lValue = lValue;
    }

    public Operator operator() {
        return operator;
    }

    public ExpressionTree expression() {
        return expression;
    }

    public void setExpression(ExpressionTree expression) {
        this.expression = expression;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (AssignmentTree) obj;
        return Objects.equals(this.lValue, that.lValue) &&
            Objects.equals(this.operator, that.operator) &&
            Objects.equals(this.expression, that.expression);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lValue, operator, expression);
    }

    @Override
    public String toString() {
        return "AssignmentTree[" +
            "lValue=" + lValue + ", " +
            "operator=" + operator + ", " +
            "expression=" + expression + ']';
    }

}
