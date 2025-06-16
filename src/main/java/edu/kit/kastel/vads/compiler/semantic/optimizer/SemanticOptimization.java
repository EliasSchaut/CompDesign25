package edu.kit.kastel.vads.compiler.semantic.optimizer;

import edu.kit.kastel.vads.compiler.parser.ast.ProgramTree;
import edu.kit.kastel.vads.compiler.parser.visitor.Unit;

public class SemanticOptimization {

    private final ProgramTree program;

    public SemanticOptimization(ProgramTree program) {
        this.program = program;
    }

    public void optimize() {
        this.program.accept(new RemoveDeadCode(), Unit.INSTANCE);
        this.program.accept(new RemoveNotNot(), Unit.INSTANCE);
        this.program.accept(new ReplaceForLoop(), Unit.INSTANCE);
        this.program.accept(new ShortCircuitEvaluation(), Unit.INSTANCE);

        // Should be after ReplaceForLoop and after RemoveDeadCode
        this.program.accept(new ReplaceWhileWithOneLoop(), 0);
    }

}
