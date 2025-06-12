package edu.kit.kastel.vads.compiler.semantic.analysis;

import edu.kit.kastel.vads.compiler.parser.ast.ProgramTree;
import edu.kit.kastel.vads.compiler.semantic.analysis.typed.ContextRecursivePostorderVisitor;
import edu.kit.kastel.vads.compiler.parser.visitor.RecursivePostorderVisitor;
import edu.kit.kastel.vads.compiler.semantic.Namespace;
import edu.kit.kastel.vads.compiler.semantic.analysis.typed.TypedAnalysis;
import edu.kit.kastel.vads.compiler.semantic.analysis.typed.TypedContext;

public class SemanticAnalysis {

    private final ProgramTree program;

    public SemanticAnalysis(ProgramTree program) {
        this.program = program;
    }

    public void analyze() {
        this.program.accept(new ContextRecursivePostorderVisitor<>(new TypedAnalysis()), new TypedContext());
        this.program.accept(new RecursivePostorderVisitor<>(new IntegerLiteralRangeAnalysis()), new Namespace<>());
//        this.program.accept(new RecursivePostorderVisitor<>(new VariableStatusAnalysis()), new Namespace<>());
        this.program.accept(new RecursivePostorderVisitor<>(new ReturnAnalysis()), new ReturnAnalysis.ReturnState());
    }

}
