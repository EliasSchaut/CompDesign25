package edu.kit.kastel.vads.compiler.parser.ast.statement.control;

import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.lexer.tokens.Keyword;
import edu.kit.kastel.vads.compiler.parser.ast.expression.ExpressionTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.StatementTree;
import edu.kit.kastel.vads.compiler.parser.visitor.Visitor;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

public final class IfTree
    implements ControlTree {
    private final Keyword ifKeyword;
    private ExpressionTree condition;
    private StatementTree thenBlock;
    private @Nullable StatementTree elseBlock;

    public IfTree(Keyword ifKeyword,
                  ExpressionTree condition,
                  StatementTree thenBlock,
                  @Nullable StatementTree elseBlock) {
        this.ifKeyword = ifKeyword;
        this.condition = condition;
        this.thenBlock = thenBlock;
        this.elseBlock = elseBlock;
    }

    @Override
    public Span span() {
        StatementTree block = elseBlock();
        return ifKeyword().span().merge(block == null
            ? thenBlock().span()
            : block.span());
    }

    @Override
    public <T, R> R accept(Visitor<T, R> visitor, T data) {
        return visitor.visit(this, data);
    }

    public Keyword ifKeyword() {
        return ifKeyword;
    }

    public ExpressionTree condition() {
        return condition;
    }

    public void setCondition(ExpressionTree condition) {
        this.condition = condition;
    }

    public StatementTree thenBlock() {
        return thenBlock;
    }

    public void setThenBlock(StatementTree thenBlock) {
        this.thenBlock = thenBlock;
    }

    public @Nullable StatementTree elseBlock() {
        return elseBlock;
    }

    public void setElseBlock(@Nullable StatementTree elseBlock) {
        this.elseBlock = elseBlock;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (IfTree) obj;
        return Objects.equals(this.ifKeyword, that.ifKeyword) &&
            Objects.equals(this.condition, that.condition) &&
            Objects.equals(this.thenBlock, that.thenBlock) &&
            Objects.equals(this.elseBlock, that.elseBlock);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ifKeyword, condition, thenBlock, elseBlock);
    }

    @Override
    public String toString() {
        return "IfTree[" +
            "ifKeyword=" + ifKeyword + ", " +
            "condition=" + condition + ", " +
            "thenBlock=" + thenBlock + ", " +
            "elseBlock=" + elseBlock + ']';
    }

}
