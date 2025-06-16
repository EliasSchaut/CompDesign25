package edu.kit.kastel.vads.compiler.semantic.optimizer;

import edu.kit.kastel.vads.compiler.lexer.tokens.Operator;
import edu.kit.kastel.vads.compiler.parser.ast.Tree;
import edu.kit.kastel.vads.compiler.parser.ast.expression.operation.UnaryOperationTree;
import edu.kit.kastel.vads.compiler.parser.visitor.NodeReplacementVisitor;
import edu.kit.kastel.vads.compiler.parser.visitor.Unit;

public class RemoveNotNot implements NodeReplacementVisitor<Unit> {
    @Override
    public Tree visit(UnaryOperationTree unaryOperationTree, Unit data) {
        if (unaryOperationTree.operator().isOperator(Operator.OperatorType.NOT)) {
            var inner = unaryOperationTree.operand();
            if (inner instanceof UnaryOperationTree innerUnaryOperationTree &&
                innerUnaryOperationTree.operator().isOperator(Operator.OperatorType.NOT)) {
                // Remove double negation
                return visit(innerUnaryOperationTree.operand(), data);
            }
        }

        return unaryOperationTree;
    }
}
