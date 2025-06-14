package edu.kit.kastel.vads.compiler.semantic.analysis;

import edu.kit.kastel.vads.compiler.parser.ast.NameTree;
import edu.kit.kastel.vads.compiler.parser.ast.TypeTree;
import edu.kit.kastel.vads.compiler.parser.ast.expression.BooleanTree;
import edu.kit.kastel.vads.compiler.parser.ast.expression.LiteralTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.control.BreakTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.control.ContinueTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.control.ForTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.control.WhileTree;
import edu.kit.kastel.vads.compiler.parser.visitor.AggregateVisitor;
import edu.kit.kastel.vads.compiler.semantic.SemanticException;

public class BreakContinueAnalysis implements AggregateVisitor<BreakContinueAnalysis.BreakContinueState, BreakContinueAnalysis.BreakContinueState> {
    public record BreakContinueState(boolean inLoop) {}

    @Override
    public BreakContinueState aggregate(BreakContinueState data, BreakContinueState value) {
        return new BreakContinueState(data.inLoop || value.inLoop);
    }

    @Override
    public BreakContinueState defaultData() {
        return new BreakContinueState(false);
    }

    @Override
    public BreakContinueState visit(ForTree forTree, BreakContinueState data) {
        AggregateVisitor.super.visit(forTree, new BreakContinueState(true));
        return data;
    }

    @Override
    public BreakContinueAnalysis.BreakContinueState visit(WhileTree whileTree, BreakContinueState data) {
        AggregateVisitor.super.visit(whileTree, new BreakContinueState(true));
        return data;
    }

    @Override
    public BreakContinueAnalysis.BreakContinueState visit(BreakTree breakTree, BreakContinueState data) {
        if (!data.inLoop) {
            throw new SemanticException("Break statement outside of loop at " + breakTree.span());
        }
        return data;
    }

    @Override
    public BreakContinueAnalysis.BreakContinueState visit(ContinueTree continueTree, BreakContinueState data) {
        if (!data.inLoop) {
            throw new SemanticException("Continue statement outside of loop at " + continueTree.span());
        }
        return data;
    }

    @Override
    public BreakContinueState visit(BooleanTree booleanTree, BreakContinueState data) {
        return data;
    }

    @Override
    public BreakContinueState visit(LiteralTree literalTree, BreakContinueState data) {
        return data;
    }

    @Override
    public BreakContinueState visit(NameTree nameTree, BreakContinueState data) {
        return data;
    }

    @Override
    public BreakContinueState visit(TypeTree typeTree, BreakContinueState data) {
        return data;
    }
}
