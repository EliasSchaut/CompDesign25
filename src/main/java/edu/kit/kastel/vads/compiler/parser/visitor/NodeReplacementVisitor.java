package edu.kit.kastel.vads.compiler.parser.visitor;

import edu.kit.kastel.vads.compiler.parser.ast.FunctionTree;
import edu.kit.kastel.vads.compiler.parser.ast.NameTree;
import edu.kit.kastel.vads.compiler.parser.ast.ProgramTree;
import edu.kit.kastel.vads.compiler.parser.ast.Tree;
import edu.kit.kastel.vads.compiler.parser.ast.TypeTree;
import edu.kit.kastel.vads.compiler.parser.ast.expression.BooleanTree;
import edu.kit.kastel.vads.compiler.parser.ast.expression.ExpressionTree;
import edu.kit.kastel.vads.compiler.parser.ast.expression.IdentExpressionTree;
import edu.kit.kastel.vads.compiler.parser.ast.expression.LiteralTree;
import edu.kit.kastel.vads.compiler.parser.ast.expression.operation.BinaryOperationTree;
import edu.kit.kastel.vads.compiler.parser.ast.expression.operation.TernaryOperationTree;
import edu.kit.kastel.vads.compiler.parser.ast.expression.operation.UnaryOperationTree;
import edu.kit.kastel.vads.compiler.parser.ast.lvalue.LValueIdentTree;
import edu.kit.kastel.vads.compiler.parser.ast.lvalue.LValueTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.AssignmentTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.BlockTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.DeclarationTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.StatementTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.control.BreakTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.control.ContinueTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.control.ForTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.control.IfTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.control.ReturnTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.control.WhileTree;

public interface NodeReplacementVisitor<T> extends Visitor<T, Tree> {

    @Override
    default Tree visit(AssignmentTree assignmentTree, T data) {
        assignmentTree.setLvalue((LValueTree) visit(assignmentTree.lValue(), data));
        assignmentTree.setExpression((ExpressionTree) visit(assignmentTree.expression(), data));
        return assignmentTree;
    }

    @Override
    default Tree visit(BinaryOperationTree binaryOperationTree, T data) {
        binaryOperationTree.setLhs((ExpressionTree) visit(binaryOperationTree.lhs(), data));
        binaryOperationTree.setRhs((ExpressionTree) visit(binaryOperationTree.rhs(), data));
        return binaryOperationTree;
    }

    @Override
    default Tree visit(BlockTree blockTree, T data) {
        for (int i = 0; i < blockTree.statements().size(); i++) {
            StatementTree statement = blockTree.statements().get(i);
            blockTree.setStatement(i, (StatementTree) statement.accept(this, data));
        }
        return blockTree;
    }

    @Override
    default Tree visit(DeclarationTree declarationTree, T data) {
        declarationTree.setType((TypeTree) visit(declarationTree.type(), data));
        declarationTree.setName((NameTree) visit(declarationTree.name(), data));
        if (declarationTree.initializer() != null) {
            declarationTree.setInitializer((ExpressionTree) visit(declarationTree.initializer(), data));
        }
        return declarationTree;
    }

    @Override
    default Tree visit(ForTree forTree, T data) {
        if (forTree.init() != null) {
            forTree.setInit((StatementTree) visit(forTree.init(), data));
        }
        forTree.setCondition((ExpressionTree) visit(forTree.condition(), data));
        forTree.setBody((StatementTree) visit(forTree.body(), data));
        if (forTree.update() != null) {
            forTree.setUpdate((StatementTree) visit(forTree.update(), data));
        }
        return forTree;
    }

    @Override
    default Tree visit(FunctionTree functionTree, T data) {
        functionTree.setReturnType((TypeTree) visit(functionTree.returnType(), data));
        functionTree.setName((NameTree) visit(functionTree.name(), data));
        functionTree.setBody((BlockTree) visit(functionTree.body(), data));
        return functionTree;
    }

    @Override
    default Tree visit(IdentExpressionTree identExpressionTree, T data) {
        identExpressionTree.setName((NameTree) visit(identExpressionTree.name(), data));
        return identExpressionTree;
    }

    @Override
    default Tree visit(IfTree ifTree, T data) {
        ifTree.setCondition((ExpressionTree) visit(ifTree.condition(), data));
        ifTree.setThenBlock((StatementTree) visit(ifTree.thenBlock(), data));
        if (ifTree.elseBlock() != null) {
            ifTree.setElseBlock((StatementTree) visit(ifTree.elseBlock(), data));
        }
        return ifTree;
    }

    @Override
    default Tree visit(LValueIdentTree lValueIdentTree, T data) {
        lValueIdentTree.setName((NameTree) visit(lValueIdentTree.name(), data));
        return lValueIdentTree;
    }

    @Override
    default Tree visit(ProgramTree programTree, T data) {
        for (int i = 0; i < programTree.topLevelTrees().size(); i++) {
            FunctionTree tree = programTree.topLevelTrees().get(i);
            programTree.setFunction(i, (FunctionTree) visit(tree, data));
        }
        return programTree;
    }

    @Override
    default Tree visit(ReturnTree returnTree, T data) {
        returnTree.setExpression((ExpressionTree) visit(returnTree.expression(), data));
        return returnTree;
    }

    @Override
    default Tree visit(TernaryOperationTree ternaryOperationTree, T data) {
        ternaryOperationTree.setCondition((ExpressionTree) visit(ternaryOperationTree.condition(), data));
        ternaryOperationTree.setTrueBranch((ExpressionTree) visit(ternaryOperationTree.trueBranch(), data));
        ternaryOperationTree.setFalseBranch((ExpressionTree) visit(ternaryOperationTree.falseBranch(), data));
        return ternaryOperationTree;
    }

    @Override
    default Tree visit(UnaryOperationTree unaryOperationTree, T data) {
        unaryOperationTree.setOperand((ExpressionTree) visit(unaryOperationTree.operand(), data));
        return unaryOperationTree;
    }

    @Override
    default Tree visit(WhileTree whileTree, T data) {
        whileTree.setCondition((ExpressionTree) visit(whileTree.condition(), data));
        whileTree.setBody((StatementTree) visit(whileTree.body(), data));
        return whileTree;
    }

    @Override
    default Tree visit(BooleanTree booleanTree, T data) {
        return booleanTree;
    }

    @Override
    default Tree visit(BreakTree breakTree, T data) {
        return breakTree;
    }

    @Override
    default Tree visit(ContinueTree continueTree, T data) {
        return continueTree;
    }

    @Override
    default Tree visit(LiteralTree literalTree, T data) {
        return literalTree;
    }

    @Override
    default Tree visit(NameTree nameTree, T data) {
        return nameTree;
    }

    @Override
    default Tree visit(TypeTree typeTree, T data) {
        return typeTree;
    }
}
