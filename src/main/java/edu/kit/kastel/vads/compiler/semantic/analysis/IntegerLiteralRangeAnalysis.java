package edu.kit.kastel.vads.compiler.semantic.analysis;

import edu.kit.kastel.vads.compiler.parser.ast.expression.LiteralTree;
import edu.kit.kastel.vads.compiler.parser.visitor.NoOpVisitor;
import edu.kit.kastel.vads.compiler.parser.visitor.Unit;
import edu.kit.kastel.vads.compiler.semantic.Namespace;
import edu.kit.kastel.vads.compiler.semantic.SemanticException;

public class IntegerLiteralRangeAnalysis implements NoOpVisitor<Namespace<Void>> {

    @Override
    public Unit visit(LiteralTree literalTree, Namespace<Void> data) {
      literalTree.parseValue()
          .orElseThrow(
              () -> new SemanticException("invalid integer literal " + literalTree.value())
          );
        return NoOpVisitor.super.visit(literalTree, data);
    }
}
