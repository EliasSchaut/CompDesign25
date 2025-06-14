package edu.kit.kastel.vads.compiler.semantic.analysis;

import edu.kit.kastel.vads.compiler.parser.ast.FunctionTree;
import edu.kit.kastel.vads.compiler.parser.ast.NameTree;
import edu.kit.kastel.vads.compiler.parser.ast.TypeTree;
import edu.kit.kastel.vads.compiler.parser.ast.expression.BooleanTree;
import edu.kit.kastel.vads.compiler.parser.ast.expression.LiteralTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.control.BreakTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.control.ContinueTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.control.ForTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.control.IfTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.control.ReturnTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.control.WhileTree;
import edu.kit.kastel.vads.compiler.parser.visitor.AggregateVisitor;
import edu.kit.kastel.vads.compiler.semantic.SemanticException;

/// Checks that functions return.
/// Currently only works for straight-line code.
class ReturnAnalysis implements AggregateVisitor<ReturnAnalysis.ReturnState, ReturnAnalysis.ReturnState> {
    private final boolean includeContinueAndBreak;

    static class ReturnState {
        boolean returns = false;
    }

    public ReturnAnalysis(boolean includeContinueAndBreak) {
        this.includeContinueAndBreak = includeContinueAndBreak;
    }

    @Override
    public ReturnState visit(ReturnTree returnTree, ReturnState data) {
        data.returns = true;
        return data;
    }

    @Override
    public ReturnState visit(WhileTree whileTree, ReturnState data) {
        // Cannot return because we cannot ensure that the loop ever runs
        return new ReturnState();
    }

    @Override
    public ReturnState visit(ForTree forTree, ReturnState data) {
        // Cannot return because we cannot ensure that the loop ever runs
        return new ReturnState();
    }

    @Override
    public ReturnState aggregate(ReturnState data, ReturnState value) {
        return value;
    }

    @Override
    public ReturnState defaultData() {
        return new ReturnState();
    }

    @Override
    public ReturnState visit(FunctionTree functionTree, ReturnState data) {

        data = visit(functionTree.body(), data);

        if (!data.returns) {
            throw new SemanticException("function " + functionTree.name() + " does not return");
        }
        data.returns = false;
        return data;
    }

    @Override
    public ReturnState visit(IfTree ifTree, ReturnState data) {
        var elseBlock = ifTree.elseBlock();
        if (elseBlock != null) {
            ReturnState thenState = new ReturnState();
            ReturnState elseState = new ReturnState();

            visit(ifTree.thenBlock(), thenState);
            visit(elseBlock, elseState);

            data.returns = thenState.returns && elseState.returns;
        }

        return data;
    }

    @Override
    public ReturnState visit(BooleanTree booleanTree, ReturnState data) {
        return data;
    }

    @Override
    public ReturnState visit(BreakTree breakTree, ReturnState data) {
        if (includeContinueAndBreak) {
            data.returns = true;
        }
        return data;
    }

    @Override
    public ReturnState visit(ContinueTree continueTree, ReturnState data) {
        if (includeContinueAndBreak) {
            data.returns = true;
        }
        return data;
    }

    @Override
    public ReturnState visit(LiteralTree literalTree, ReturnState data) {
        return data;
    }

    @Override
    public ReturnState visit(NameTree nameTree, ReturnState data) {
        return data;
    }

    @Override
    public ReturnState visit(TypeTree typeTree, ReturnState data) {
        return data;
    }
}
