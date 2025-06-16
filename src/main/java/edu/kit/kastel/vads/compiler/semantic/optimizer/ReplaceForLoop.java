package edu.kit.kastel.vads.compiler.semantic.optimizer;

import edu.kit.kastel.vads.compiler.lexer.tokens.Keyword;
import edu.kit.kastel.vads.compiler.parser.ast.NameTree;
import edu.kit.kastel.vads.compiler.parser.ast.TypeTree;
import edu.kit.kastel.vads.compiler.parser.ast.expression.BooleanTree;
import edu.kit.kastel.vads.compiler.parser.ast.expression.LiteralTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.BlockTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.StatementTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.control.BreakTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.control.ContinueTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.control.ForTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.control.WhileTree;
import edu.kit.kastel.vads.compiler.parser.visitor.AggregateVisitor;
import edu.kit.kastel.vads.compiler.parser.visitor.Unit;
import java.util.List;

public class ReplaceForLoop implements AggregateVisitor<Unit, Unit> {
    @Override
    public Unit visit(BlockTree blockTree, Unit data) {
        for (StatementTree statement : blockTree.statements()) {
            if (statement instanceof ForTree forTree) {
                // Replace ForTree with WhileTree in the block
                StatementTree update = forTree.update();
                var whileTree = new WhileTree(
                    new Keyword(Keyword.KeywordType.WHILE, forTree.forKeyword().span()),
                    forTree.condition(),
                    update == null
                        ? forTree.body()
                        : new BlockTree(List.of(forTree.body(), update),
                        forTree.body().span().merge(update.span()))
                );

                StatementTree transformedTree;
                var init = forTree.init();
                if (init != null) {
                    transformedTree = new BlockTree(List.of(init, whileTree), init.span().merge(whileTree.span()));
                } else {
                    transformedTree = whileTree;
                }

                // Replace the ForTree with WhileTree in the block's statements
                blockTree.setStatement(blockTree.statements().indexOf(forTree), transformedTree);
            }

            // Recursively visit other statements
            statement.accept(this, data);
        }

        return Unit.INSTANCE;
    }

    @Override
    public Unit visit(BooleanTree booleanTree, Unit data) {
        return Unit.INSTANCE;
    }

    @Override
    public Unit visit(BreakTree breakTree, Unit data) {
        return Unit.INSTANCE;
    }

    @Override
    public Unit visit(ContinueTree continueTree, Unit data) {
        return Unit.INSTANCE;
    }

    @Override
    public Unit visit(LiteralTree literalTree, Unit data) {
        return Unit.INSTANCE;
    }

    @Override
    public Unit visit(NameTree nameTree, Unit data) {
        return Unit.INSTANCE;
    }

    @Override
    public Unit visit(TypeTree typeTree, Unit data) {
        return Unit.INSTANCE;
    }

    @Override
    public Unit aggregate(Unit data, Unit value) {
        return Unit.INSTANCE;
    }

    @Override
    public Unit defaultData() {
        return Unit.INSTANCE;
    }
}
