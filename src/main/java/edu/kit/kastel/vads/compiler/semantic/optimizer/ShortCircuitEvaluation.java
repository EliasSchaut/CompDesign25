package edu.kit.kastel.vads.compiler.semantic.optimizer;

import edu.kit.kastel.vads.compiler.parser.ast.Tree;
import edu.kit.kastel.vads.compiler.parser.ast.expression.BooleanTree;
import edu.kit.kastel.vads.compiler.parser.ast.expression.ExpressionTree;
import edu.kit.kastel.vads.compiler.parser.ast.expression.operation.BinaryOperationTree;
import edu.kit.kastel.vads.compiler.parser.ast.expression.operation.TernaryOperationTree;
import edu.kit.kastel.vads.compiler.parser.visitor.NodeReplacementVisitor;
import edu.kit.kastel.vads.compiler.parser.visitor.Unit;

public class ShortCircuitEvaluation implements NodeReplacementVisitor<Unit> {
    @Override
    public Tree visit(BinaryOperationTree binaryOperationTree, Unit data) {
        var left = (ExpressionTree) visit(binaryOperationTree.lhs(), data);
        var right = (ExpressionTree) visit(binaryOperationTree.rhs(), data);

        switch (binaryOperationTree.operatorType()) {
            case AND -> {
                // Use ternary operator for short-circuit evaluation
                // A && B becomes A ? B : false

                return new TernaryOperationTree(left,
                    right,
                    new BooleanTree(false, left.span().merge(right.span())));
            }
            case OR -> {
                // Use ternary operator for short-circuit evaluation
                // A || B becomes A ? true : B

                return new TernaryOperationTree(left,
                    new BooleanTree(true, left.span().merge(right.span())),
                    right);
            }
            default -> {
                return binaryOperationTree;
            }
        }
    }
}
