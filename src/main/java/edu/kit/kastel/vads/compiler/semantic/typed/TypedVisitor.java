package edu.kit.kastel.vads.compiler.semantic.typed;

import edu.kit.kastel.vads.compiler.parser.ast.FunctionTree;
import edu.kit.kastel.vads.compiler.parser.ast.NameTree;
import edu.kit.kastel.vads.compiler.parser.ast.ProgramTree;
import edu.kit.kastel.vads.compiler.parser.ast.TypeTree;
import edu.kit.kastel.vads.compiler.parser.ast.expression.BooleanTree;
import edu.kit.kastel.vads.compiler.parser.ast.expression.IdentExpressionTree;
import edu.kit.kastel.vads.compiler.parser.ast.expression.LiteralTree;
import edu.kit.kastel.vads.compiler.parser.ast.expression.operation.BinaryOperationTree;
import edu.kit.kastel.vads.compiler.parser.ast.expression.operation.TernaryOperationTree;
import edu.kit.kastel.vads.compiler.parser.ast.expression.operation.UnaryOperationTree;
import edu.kit.kastel.vads.compiler.parser.ast.lvalue.LValueIdentTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.AssignmentTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.BlockTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.DeclarationTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.control.BreakTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.control.ContinueTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.control.ForTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.control.IfTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.control.ReturnTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.control.WhileTree;
import edu.kit.kastel.vads.compiler.semantic.analysis.typed.TypedTree;

public interface TypedVisitor<T, R> {

    R visitAssignment(TypedTree<AssignmentTree> assignmentTree, T data);

    R visitBinaryOperation(TypedTree<BinaryOperationTree> binaryOperationTree, T data);

    R visitBlock(TypedTree<BlockTree> blockTree, T data);

    R visitBoolean(TypedTree<BooleanTree> booleanTree, T data);

    R visitBreak(TypedTree<BreakTree> breakTree, T data);

    R visitContinue(TypedTree<ContinueTree> continueTree, T data);

    R visitDeclaration(TypedTree<DeclarationTree> declarationTree, T data);

    R visitFor(TypedTree<ForTree> forTree, T data);

    R visitFunction(TypedTree<FunctionTree> functionTree, T data);

    R visitIdentExpression(TypedTree<IdentExpressionTree> identExpressionTree, T data);

    R visitIf(TypedTree<IfTree> ifTree, T data);

    R visitLiteral(TypedTree<LiteralTree> literalTree, T data);

    R visitLValueIdent(TypedTree<LValueIdentTree> lValueIdentTree, T data);

    R visitName(TypedTree<NameTree> nameTree, T data);

    R visitProgram(TypedTree<ProgramTree> programTree, T data);

    R visitReturn(TypedTree<ReturnTree> returnTree, T data);

    R visitTernaryOperation(TypedTree<TernaryOperationTree> ternaryOperationTree, T data);

    R visitType(TypedTree<TypeTree> typeTree, T data);

    R visitUnaryOperation(TypedTree<UnaryOperationTree> unaryOperationTree, T data);

    R visitWhile(TypedTree<WhileTree> whileTree, T data);
}
