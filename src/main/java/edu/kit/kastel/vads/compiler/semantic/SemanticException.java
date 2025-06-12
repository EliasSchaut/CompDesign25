package edu.kit.kastel.vads.compiler.semantic;

import edu.kit.kastel.vads.compiler.parser.ast.Tree;

public class SemanticException extends RuntimeException {
    public SemanticException(String message) {
        super(message);
    }

    public SemanticException(Tree tree, String message) {
        super(tree.getClass().getSimpleName() + " " + tree.span() + ": " + message);
    }
}
