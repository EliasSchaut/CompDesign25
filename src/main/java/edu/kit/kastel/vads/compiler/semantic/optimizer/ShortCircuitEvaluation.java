package edu.kit.kastel.vads.compiler.semantic.optimizer;

import edu.kit.kastel.vads.compiler.parser.ast.Tree;
import edu.kit.kastel.vads.compiler.parser.ast.expression.BooleanTree;
import edu.kit.kastel.vads.compiler.parser.ast.expression.operation.BinaryOperationTree;
import edu.kit.kastel.vads.compiler.parser.ast.expression.operation.TernaryOperationTree;
import edu.kit.kastel.vads.compiler.parser.visitor.NodeReplacementVisitor;
import edu.kit.kastel.vads.compiler.parser.visitor.Unit;

public class ShortCircuitEvaluation implements NodeReplacementVisitor<Unit> {
    @Override
    public Tree visit(BinaryOperationTree binaryOperationTree, Unit data) {
        switch (binaryOperationTree.operatorType()) {
            case AND -> {
                // Use ternary operator for short-circuit evaluation
                // A && B becomes A ? B : false

                var left = binaryOperationTree.lhs();
                var right = binaryOperationTree.rhs();
                return new TernaryOperationTree(left,
                    right,
                    new BooleanTree(false, left.span().merge(right.span())));
            }
            case OR -> {
                // Use ternary operator for short-circuit evaluation
                // A || B becomes A ? true : B

                var left = binaryOperationTree.lhs();
                var right = binaryOperationTree.rhs();
                return new TernaryOperationTree(left,
                    new BooleanTree(true, left.span().merge(right.span())),
                    right);
            }
        }

        return binaryOperationTree;
    }
}
