package edu.kit.kastel.vads.compiler.semantic.optimizer;

import edu.kit.kastel.vads.compiler.lexer.tokens.Keyword;
import edu.kit.kastel.vads.compiler.parser.ast.Tree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.BlockTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.StatementTree;
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

        visit(forTree.body(), data);

        StatementTree body;
        if (update == null) {
            body = forTree.body();
        } else {
            if (forTree.body() instanceof BlockTree bodyBlockTree) {
                bodyBlockTree.addStatement(update);
                body = bodyBlockTree;
            } else {
                body = new BlockTree(List.of(forTree.body(), update), forTree.body().span().merge(update.span()));
            }
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
}
