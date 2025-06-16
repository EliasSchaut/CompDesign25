package edu.kit.kastel.vads.compiler.semantic.analysis;

import edu.kit.kastel.vads.compiler.lexer.tokens.Keyword;
import edu.kit.kastel.vads.compiler.parser.ast.FunctionTree;
import edu.kit.kastel.vads.compiler.parser.ast.ProgramTree;
import edu.kit.kastel.vads.compiler.parser.visitor.NoOpVisitor;
import edu.kit.kastel.vads.compiler.parser.visitor.Unit;
import edu.kit.kastel.vads.compiler.semantic.SemanticException;

public class MainMethodAnalysis implements NoOpVisitor<Unit> {
    @Override
    public Unit visit(ProgramTree programTree, Unit data) {
        var foundMainMethod = false;
        for (FunctionTree functionTree : programTree.topLevelTrees()) {
            if (functionTree.returnType().type().asString().equals(Keyword.KeywordType.INT.keyword())
                && functionTree.name().name().asString().equals("main")
                && functionTree.parameters().isEmpty()) {
                foundMainMethod = true;
            }
        }

        if (!foundMainMethod) {
            throw new SemanticException("No main method found in the program. "
                + "A main method is required to run the program.");
        }

        return data;
    }
}
