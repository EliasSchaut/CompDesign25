package edu.kit.kastel.vads.compiler.semantic.analysis;

import edu.kit.kastel.vads.compiler.parser.ast.ProgramTree;
import edu.kit.kastel.vads.compiler.parser.visitor.RecursivePostorderVisitor;
import edu.kit.kastel.vads.compiler.semantic.Namespace;
import edu.kit.kastel.vads.compiler.parser.visitor.ScopedContext;

public class SemanticAnalysis {

    private final ProgramTree program;

    public SemanticAnalysis(ProgramTree program) {
        this.program = program;
    }

    public void analyze() {
        this.program.accept(new TypeAnalysis(), new ScopedContext<>(new TypeAnalysis.TypeContext()));
        this.program.accept(new RecursivePostorderVisitor<>(new IntegerLiteralRangeAnalysis()), new Namespace<>());
//        this.program.accept(new RecursivePostorderVisitor<>(new VariableStatusAnalysis()), new Namespace<>());
        this.program.accept(new RecursivePostorderVisitor<>(new IllegalDeclarationAnalysis()), new Namespace<>());
        this.program.accept(new BreakContinueAnalysis(), new BreakContinueAnalysis.BreakContinueState(false));
        this.program.accept(new ReturnAnalysis(false), new ReturnAnalysis.ReturnState());
    }

}
