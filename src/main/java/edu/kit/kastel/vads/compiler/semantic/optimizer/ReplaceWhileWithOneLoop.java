package edu.kit.kastel.vads.compiler.semantic.optimizer;

import edu.kit.kastel.vads.compiler.lexer.tokens.Keyword;
import edu.kit.kastel.vads.compiler.parser.ast.NameTree;
import edu.kit.kastel.vads.compiler.parser.ast.TypeTree;
import edu.kit.kastel.vads.compiler.parser.ast.expression.BooleanTree;
import edu.kit.kastel.vads.compiler.parser.ast.expression.LiteralTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.AssignmentTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.BlockTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.DeclarationTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.StatementTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.control.BreakTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.control.ContinueTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.control.ControlTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.control.ForTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.control.IfTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.control.ReturnTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.control.WhileTree;
import edu.kit.kastel.vads.compiler.parser.visitor.AggregateVisitor;
import java.lang.Boolean;
import java.util.ArrayList;
import java.util.List;

public class ReplaceWhileWithOneLoop implements AggregateVisitor<Boolean, Boolean> {
    @Override
    public Boolean visit(BlockTree blockTree, Boolean data) {
        List<StatementTree> blockStatements = blockTree.statements();
        for (int i = 0; i < blockStatements.size(); i++) {
            var statement = blockStatements.get(i);

            if (statement instanceof WhileTree whileTree) {
                switch (whileTree.body()) {
                    case AssignmentTree _,  DeclarationTree _ -> statement.accept(this, data);
                    case BlockTree whileBlock -> {
                        // Optimization is not possible where there is somewhere a continue statement nested in the while loop
                        var containsContinue = whileBlock.accept(this, data);
                        if (containsContinue) continue;

                        for (int j = 0; j < whileBlock.statements().size(); j++) {
                            var innerStatement = whileBlock.statements().get(j);
                            // If we break directly, we can remove the while loop and instead create an empty if tree
                            if (innerStatement instanceof BreakTree) {
                                blockTree.setStatement(i, getIfTree(whileTree, whileBlock.statements().subList(0, j)));
                                break;
                            } else if (innerStatement instanceof ReturnTree) {
                                blockTree.setStatement(i, getIfTree(whileTree, whileBlock.statements()));
                                break;
                            } else {
                                innerStatement.accept(this, data);
                            }
                        }
                    }
                    case ControlTree controlTree -> {
                        switch (controlTree) {
                            // If we return or break directly, we can remove the while loop and instead create an empty if tree
                            case BreakTree _ ->
                                blockTree.setStatement(i, getIfTree(whileTree, List.of()));
                            case ReturnTree returnTree ->
                                blockTree.setStatement(i, getIfTree(whileTree, List.of(returnTree)));
                            // Otherwise we can keep the while loop and continue visiting the statements
                            case ContinueTree _, IfTree _, ForTree _, WhileTree _ ->
                                statement.accept(this, data);
                        }
                    }
                }
            } else {
                // Recursively visit other statements
                statement.accept(this, data);
            }
        }

        return blockTree.statements()
            .stream()
            .map(s -> s.accept(this, data))
            .reduce(Boolean::logicalOr)
            .orElse(false);
    }

    private static IfTree getIfTree(WhileTree whileTree, List<StatementTree> bodyStatements) {
        return new IfTree(
            new Keyword(Keyword.KeywordType.IF, whileTree.whileKeyword().span()),
            whileTree.condition(),
            new BlockTree(new ArrayList<>(bodyStatements), whileTree.body().span()),
            null
        );
    }

    @Override
    public Boolean visit(BooleanTree booleanTree, java.lang.Boolean data) {
        return false;
    }

    @Override
    public Boolean visit(BreakTree breakTree, Boolean data) {
        return false;
    }

    @Override
    public Boolean visit(ContinueTree continueTree, Boolean data) {
        return true;
    }

    @Override
    public Boolean visit(LiteralTree literalTree, Boolean data) {
        return false;
    }

    @Override
    public Boolean visit(NameTree nameTree, Boolean data) {
        return false;
    }

    @Override
    public Boolean visit(TypeTree typeTree, Boolean data) {
        return false;
    }

    @Override
    public Boolean aggregate(Boolean data, Boolean value) {
        return data || value;
    }

    @Override
    public Boolean defaultData() {
        return false;
    }
}
