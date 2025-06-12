package edu.kit.kastel.vads.compiler.parser.ast.expression;

import edu.kit.kastel.vads.compiler.parser.ast.Tree;
import edu.kit.kastel.vads.compiler.parser.ast.expression.operation.BinaryOperationTree;
import edu.kit.kastel.vads.compiler.parser.ast.expression.operation.TernaryOperationTree;
import edu.kit.kastel.vads.compiler.parser.ast.expression.operation.UnaryOperationTree;

public sealed interface ExpressionTree extends Tree
    permits BinaryOperationTree, BooleanTree, IdentExpressionTree, LiteralTree,
    TernaryOperationTree, UnaryOperationTree {
}
