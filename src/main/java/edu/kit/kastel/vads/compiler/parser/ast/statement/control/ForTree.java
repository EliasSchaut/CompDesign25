package edu.kit.kastel.vads.compiler.parser.ast.statement.control;

import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.lexer.tokens.Keyword;
import edu.kit.kastel.vads.compiler.parser.ast.expression.ExpressionTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.StatementTree;
import edu.kit.kastel.vads.compiler.parser.visitor.Visitor;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

public final class ForTree implements ControlTree {
    private final Keyword forKeyword;
    private @Nullable StatementTree init;
    private ExpressionTree condition;
    private @Nullable StatementTree update;
    private StatementTree body;

    public ForTree(Keyword forKeyword,
                   @Nullable StatementTree init,
                   ExpressionTree condition,
                   @Nullable StatementTree update,
                   StatementTree body) {
        this.forKeyword = forKeyword;
        this.init = init;
        this.condition = condition;
        this.update = update;
        this.body = body;
    }

    @Override
    public Span span() {
        return forKeyword.span().merge(body.span());
    }

    @Override
    public <T, R> R accept(Visitor<T, R> visitor, T data) {
        return visitor.visit(this, data);
    }

    public Keyword forKeyword() {
        return forKeyword;
    }

    public @Nullable StatementTree init() {
        return init;
    }

    public void setInit(@Nullable StatementTree init) {
        this.init = init;
    }

    public ExpressionTree condition() {
        return condition;
    }

    public void setCondition(ExpressionTree condition) {
        this.condition = condition;
    }

    public @Nullable StatementTree update() {
        return update;
    }

    public void setUpdate(@Nullable StatementTree update) {
        this.update = update;
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
        var that = (ForTree) obj;
        return Objects.equals(this.forKeyword, that.forKeyword) &&
            Objects.equals(this.init, that.init) &&
            Objects.equals(this.condition, that.condition) &&
            Objects.equals(this.update, that.update) &&
            Objects.equals(this.body, that.body);
    }

    @Override
    public int hashCode() {
        return Objects.hash(forKeyword, init, condition, update, body);
    }

    @Override
    public String toString() {
        return "ForTree[" +
            "forKeyword=" + forKeyword + ", " +
            "init=" + init + ", " +
            "condition=" + condition + ", " +
            "update=" + update + ", " +
            "body=" + body + ']';
    }

}
