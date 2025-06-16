package edu.kit.kastel.vads.compiler.semantic.optimizer;

import edu.kit.kastel.vads.compiler.parser.ast.ProgramTree;
import edu.kit.kastel.vads.compiler.parser.visitor.Unit;

public class SemanticOptimization {

    private final ProgramTree program;

    public SemanticOptimization(ProgramTree program) {
        this.program = program;
    }

    public void optimize() {
        this.program.accept(new ReplaceForLoop(), Unit.INSTANCE);
    }

}
