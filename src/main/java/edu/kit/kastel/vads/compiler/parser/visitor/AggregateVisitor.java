package edu.kit.kastel.vads.compiler.parser.visitor;

import edu.kit.kastel.vads.compiler.parser.ast.FunctionTree;
import edu.kit.kastel.vads.compiler.parser.ast.ProgramTree;
import edu.kit.kastel.vads.compiler.parser.ast.expression.ExpressionTree;
import edu.kit.kastel.vads.compiler.parser.ast.expression.IdentExpressionTree;
import edu.kit.kastel.vads.compiler.parser.ast.expression.operation.BinaryOperationTree;
import edu.kit.kastel.vads.compiler.parser.ast.expression.operation.TernaryOperationTree;
import edu.kit.kastel.vads.compiler.parser.ast.expression.operation.UnaryOperationTree;
import edu.kit.kastel.vads.compiler.parser.ast.lvalue.LValueIdentTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.AssignmentTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.BlockTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.DeclarationTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.StatementTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.control.ForTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.control.IfTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.control.ReturnTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.control.WhileTree;

public interface AggregateVisitor<T, R> extends Visitor<T, R> {

    @Override
    default R visit(AssignmentTree assignmentTree, T data) {
        R r = visit(assignmentTree.lValue(), data);
        r = visit(assignmentTree.expression(), aggregate(data, r));
        return r;
    }

    @Override
    default R visit(BinaryOperationTree binaryOperationTree, T data) {
        R r = visit(binaryOperationTree.lhs(), data);
        r = visit(binaryOperationTree.rhs(), aggregate(data, r));
        return r;
    }

    @Override
    default R visit(BlockTree blockTree, T data) {
        R r = null;
        T d = data;
        for (StatementTree statement : blockTree.statements()) {
            r = statement.accept(this, d);
            d = aggregate(d, r);
        }
        return r == null
            ? defaultData()
            : r;
    }

    @Override
    default R visit(DeclarationTree declarationTree, T data) {
        R r = visit(declarationTree.type(), data);
        r = visit(declarationTree.name(), aggregate(data, r));
        ExpressionTree declaration = declarationTree.initializer();
        if (declaration != null) {
            r = visit(declaration, aggregate(data, r));
        }
        return r;
    }

    @Override
    default R visit(ForTree forTree, T data) {
        R r = null;
        StatementTree init = forTree.init();
        if (init != null) r = visit(init, data);
        r = visit(forTree.condition(), r == null ? data : aggregate(data, r));
        r = visit(forTree.body(), aggregate(data, r));
        StatementTree update = forTree.update();
        if (update != null) r = visit(update, aggregate(data, r));
        return r;
    }

    @Override
    default R visit(FunctionTree functionTree, T data) {
        R r = visit(functionTree.returnType(), data);
        r = visit(functionTree.name(), aggregate(data, r));
        r = visit(functionTree.body(), aggregate(data, r));
        return r;
    }

    @Override
    default R visit(IdentExpressionTree identExpressionTree, T data) {
        return visit(identExpressionTree.name(), data);
    }

    @Override
    default R visit(IfTree ifTree, T data) {
        R r = visit(ifTree.condition(), data);
        r = visit(ifTree.thenBlock(), aggregate(data, r));
        StatementTree elseBlock = ifTree.elseBlock();
        if (elseBlock != null) {
            r = visit(elseBlock, aggregate(data, r));
        }
        return r;
    }

    @Override
    default R visit(LValueIdentTree lValueIdentTree, T data) {
        return visit(lValueIdentTree.name(), data);
    }

    @Override
    default R visit(ProgramTree programTree, T data) {
        R r = null;
        T d = data;
        for (FunctionTree tree : programTree.topLevelTrees()) {
            r = visit(tree, d);
            d = aggregate(data, r);
        }
        return r == null
            ? defaultData()
            : r;
    }

    @Override
    default R visit(ReturnTree returnTree, T data) {
        return visit(returnTree.expression(), data);
    }

    @Override
    default R visit(TernaryOperationTree ternaryOperationTree, T data) {
        R r = visit(ternaryOperationTree.condition(), data);
        r = visit(ternaryOperationTree.trueBranch(), aggregate(data, r));
        r = visit(ternaryOperationTree.falseBranch(), aggregate(data, r));
        return r;
    }

    @Override
    default R visit(UnaryOperationTree unaryOperationTree, T data) {
        return visit(unaryOperationTree.operand(), data);
    }

    @Override
    default R visit(WhileTree whileTree, T data) {
        R r = visit(whileTree.condition(), data);
        r = visit(whileTree.body(), aggregate(data, r));
        return r;
    }

    T aggregate(T data, R value);
    R defaultData();
}
