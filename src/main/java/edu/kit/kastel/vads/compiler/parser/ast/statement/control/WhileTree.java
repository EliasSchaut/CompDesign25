package edu.kit.kastel.vads.compiler.parser.ast.statement.control;

import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.lexer.tokens.Keyword;
import edu.kit.kastel.vads.compiler.parser.ast.expression.ExpressionTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.StatementTree;
import edu.kit.kastel.vads.compiler.parser.visitor.Visitor;
import java.util.Objects;

public final class WhileTree implements ControlTree {
    private final Keyword whileKeyword;
    private ExpressionTree condition;
    private StatementTree body;

    public WhileTree(Keyword whileKeyword,
                     ExpressionTree condition,
                     StatementTree body) {
        this.whileKeyword = whileKeyword;
        this.condition = condition;
        this.body = body;
    }

    @Override
    public Span span() {
        return whileKeyword.span().merge(body.span());
    }

    @Override
    public <T, R> R accept(Visitor<T, R> visitor, T data) {
        return visitor.visit(this, data);
    }

    public Keyword whileKeyword() {
        return whileKeyword;
    }

    public ExpressionTree condition() {
        return condition;
    }

    public void setCondition(ExpressionTree condition) {
        this.condition = condition;
    }

    public StatementTree body() {
        return body;
    }

    public void setBody(StatementTree body) {
        this.body = body;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (WhileTree) obj;
        return Objects.equals(this.whileKeyword, that.whileKeyword) &&
            Objects.equals(this.condition, that.condition) &&
            Objects.equals(this.body, that.body);
    }

    @Override
    public int hashCode() {
        return Objects.hash(whileKeyword, condition, body);
    }

    @Override
    public String toString() {
        return "WhileTree[" +
            "whileKeyword=" + whileKeyword + ", " +
            "condition=" + condition + ", " +
            "body=" + body + ']';
    }

}
