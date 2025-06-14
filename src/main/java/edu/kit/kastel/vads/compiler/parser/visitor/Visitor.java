package edu.kit.kastel.vads.compiler.parser.visitor;

import edu.kit.kastel.vads.compiler.parser.ast.expression.BooleanTree;
import edu.kit.kastel.vads.compiler.parser.ast.expression.ExpressionTree;
import edu.kit.kastel.vads.compiler.parser.ast.expression.operation.UnaryOperationTree;
import edu.kit.kastel.vads.compiler.parser.ast.lvalue.LValueTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.AssignmentTree;
import edu.kit.kastel.vads.compiler.parser.ast.expression.operation.BinaryOperationTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.BlockTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.DeclarationTree;
import edu.kit.kastel.vads.compiler.parser.ast.FunctionTree;
import edu.kit.kastel.vads.compiler.parser.ast.expression.IdentExpressionTree;
import edu.kit.kastel.vads.compiler.parser.ast.lvalue.LValueIdentTree;
import edu.kit.kastel.vads.compiler.parser.ast.expression.LiteralTree;
import edu.kit.kastel.vads.compiler.parser.ast.NameTree;
import edu.kit.kastel.vads.compiler.parser.ast.ProgramTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.StatementTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.control.BreakTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.control.ContinueTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.control.ControlTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.control.ForTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.control.IfTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.control.ReturnTree;
import edu.kit.kastel.vads.compiler.parser.ast.TypeTree;
import edu.kit.kastel.vads.compiler.parser.ast.expression.operation.TernaryOperationTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.control.WhileTree;

public interface Visitor<T, R> {

    R visit(AssignmentTree assignmentTree, T data);

    R visit(BinaryOperationTree binaryOperationTree, T data);

    R visit(BlockTree blockTree, T data);

    R visit(BooleanTree booleanTree, T data);

    R visit(BreakTree breakTree, T data);

    R visit(ContinueTree continueTree, T data);

    default R visit(ControlTree controlTree, T data) {
        return switch (controlTree) {
            case BreakTree breakTree -> visit(breakTree, data);
            case ContinueTree continueTree -> visit(continueTree, data);
            case ForTree forTree -> visit(forTree, data);
            case IfTree ifTree -> visit(ifTree, data);
            case ReturnTree returnTree -> visit(returnTree, data);
            case WhileTree whileTree -> visit(whileTree, data);
        };
    }

    R visit(DeclarationTree declarationTree, T data);

    default R visit(ExpressionTree expressionTree, T data) {
        return switch (expressionTree) {
            case BooleanTree booleanTree -> visit(booleanTree, data);
            case IdentExpressionTree identExpressionTree -> visit(identExpressionTree, data);
            case LiteralTree literalTree -> visit(literalTree, data);
            case UnaryOperationTree unaryOperationTree -> visit(unaryOperationTree, data);
            case BinaryOperationTree binaryOperationTree -> visit(binaryOperationTree, data);
            case TernaryOperationTree ternaryOperationTree -> visit(ternaryOperationTree, data);
        };
    }

    R visit(ForTree forTree, T data);

    R visit(FunctionTree functionTree, T data);

    R visit(IdentExpressionTree identExpressionTree, T data);

    R visit(IfTree ifTree, T data);

    R visit(LiteralTree literalTree, T data);

    R visit(LValueIdentTree lValueIdentTree, T data);

    default R visit(LValueTree lValueTree, T data) {
        return switch (lValueTree) {
            case LValueIdentTree lValueIdentTree -> visit(lValueIdentTree, data);
        };
    }

    R visit(NameTree nameTree, T data);

    R visit(ProgramTree programTree, T data);

    R visit(ReturnTree returnTree, T data);

    default R visit(StatementTree statementTree, T data) {
        return switch (statementTree) {
            case AssignmentTree assignmentTree -> visit(assignmentTree, data);
            case BlockTree blockTree -> visit(blockTree, data);
            case DeclarationTree declarationTree -> visit(declarationTree, data);
            case ControlTree controlTree -> controlTree.accept(this, data);
        };
    }

    R visit(TernaryOperationTree ternaryOperationTree, T data);

    R visit(TypeTree typeTree, T data);

    R visit(UnaryOperationTree unaryOperationTree, T data);

    R visit(WhileTree whileTree, T data);
}
