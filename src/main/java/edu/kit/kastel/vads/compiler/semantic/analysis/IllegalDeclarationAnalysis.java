package edu.kit.kastel.vads.compiler.semantic.analysis;

import edu.kit.kastel.vads.compiler.parser.ast.statement.DeclarationTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.control.ForTree;
import edu.kit.kastel.vads.compiler.parser.visitor.NoOpVisitor;
import edu.kit.kastel.vads.compiler.parser.visitor.Unit;
import edu.kit.kastel.vads.compiler.semantic.Namespace;
import edu.kit.kastel.vads.compiler.semantic.SemanticException;

public class IllegalDeclarationAnalysis implements NoOpVisitor<Namespace<Void>> {

    @Override
    public Unit visit(ForTree forTree, Namespace<Void> data) {
        if (forTree.update() instanceof DeclarationTree) {
            throw new SemanticException("For loop step must not be a declaration");
        }

        return NoOpVisitor.super.visit(forTree, data);
    }
}
