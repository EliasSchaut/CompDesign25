package edu.kit.kastel.vads.compiler.parser.ast.expression;

import edu.kit.kastel.vads.compiler.parser.ast.Tree;

public sealed interface ExpressionTree extends Tree
    permits BinaryOperationTree, IdentExpressionTree, LiteralTree, NegateTree {
}
