package edu.kit.kastel.vads.compiler.semantic.optimizer;

import edu.kit.kastel.vads.compiler.parser.ast.NameTree;
import edu.kit.kastel.vads.compiler.parser.ast.TypeTree;
import edu.kit.kastel.vads.compiler.parser.ast.expression.BooleanTree;
import edu.kit.kastel.vads.compiler.parser.ast.expression.LiteralTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.BlockTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.StatementTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.control.BreakTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.control.ContinueTree;
import edu.kit.kastel.vads.compiler.parser.visitor.AggregateVisitor;
import edu.kit.kastel.vads.compiler.parser.visitor.Unit;
import edu.kit.kastel.vads.compiler.semantic.analysis.SemanticAnalysis;
import java.util.List;

public class RemoveDeadCode implements AggregateVisitor<Unit, Unit> {
    @Override
    public Unit visit(BlockTree blockTree, Unit data) {
        List<StatementTree> statements = blockTree.statements();
        for (int i = 0; i < statements.size(); i++) {
            StatementTree statement = statements.get(i);

            // Recursively visit other statements
            statement.accept(this, data);

            if (SemanticAnalysis.containsReturnContinueBreak(statement)) {
                // Remove remaining statements after a returning statement
                while (i + 1 < statements.size()) {
                    blockTree.removeStatement(i + 1);
                }
            }
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
