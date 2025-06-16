package edu.kit.kastel.vads.compiler.semantic.optimizer;

import edu.kit.kastel.vads.compiler.lexer.tokens.Keyword;
import edu.kit.kastel.vads.compiler.parser.ast.Tree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.BlockTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.StatementTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.control.ContinueTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.control.ForTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.control.WhileTree;
import edu.kit.kastel.vads.compiler.parser.visitor.NodeReplacementVisitor;
import edu.kit.kastel.vads.compiler.parser.visitor.Unit;
import java.util.List;

public class ReplaceForLoop implements NodeReplacementVisitor<Unit> {
    @Override
    public Tree visit(ForTree forTree, Unit data) {
        // Replace ForTree with WhileTree in the block
        StatementTree update = forTree.update();

        StatementTree body = (StatementTree) visit(forTree.body(), data);
        if (update != null) {
            if (body instanceof BlockTree bodyBlockTree) {
                bodyBlockTree.addStatement(update);
            } else {
                body = new BlockTree(List.of(body, update), body.span().merge(update.span()));
            }

            new AddStatementBeforeContinue(update).visit(body, data);
        }

        var whileTree = new WhileTree(
            new Keyword(Keyword.KeywordType.WHILE, forTree.forKeyword().span()),
            forTree.condition(),
            body);

        StatementTree transformedTree;
        var init = forTree.init();
        if (init != null) {
            transformedTree = new BlockTree(List.of(init, whileTree), init.span().merge(whileTree.span()));
        } else {
            transformedTree = whileTree;
        }

        return transformedTree;
    }

    public static class AddStatementBeforeContinue implements NodeReplacementVisitor<Unit> {
        private final StatementTree update;

        public AddStatementBeforeContinue(StatementTree update) {
            this.update = update;
        }

        @Override
        public Tree visit(ForTree forTree, Unit data) {
            // Skip tree so we only add the update statement to continues inside our loop and not the nested ones
            return forTree;
        }

        @Override
        public Tree visit(WhileTree whileTree, Unit data) {
            // Skip tree so we only add the update statement to continues inside our loop and not the nested ones
            return whileTree;
        }

        @Override
        public Tree visit(ContinueTree continueTree, Unit data) {
            // Add the update statement before the continue statement
            return new BlockTree(
                List.of(update, continueTree),
                continueTree.span().merge(update.span()));
        }
    }
}
