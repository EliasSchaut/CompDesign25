package edu.kit.kastel.vads.compiler.parser.ast.statement.control;

import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.lexer.tokens.Keyword;
import edu.kit.kastel.vads.compiler.parser.ast.expression.ExpressionTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.BlockTree;
import edu.kit.kastel.vads.compiler.parser.visitor.Visitor;
import org.jspecify.annotations.Nullable;

public record IfTree(Keyword ifKeyword, ExpressionTree condition, BlockTree thenBlock, @Nullable BlockTree elseBlock)
    implements ControlTree {
    @Override
    public Span span() {
        BlockTree elseBlock = elseBlock();
        if (elseBlock != null) {
            return ifKeyword().span().merge(elseBlock.span());
        }
        return ifKeyword().span().merge(thenBlock().span());
    }

    @Override
    public <T, R> R accept(Visitor<T, R> visitor, T data) {
        return visitor.visit(this, data);
    }
}
