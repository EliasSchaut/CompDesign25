package edu.kit.kastel.vads.compiler.semantic.analysis.typed;

import static edu.kit.kastel.vads.compiler.parser.type.BasicType.BOOL;
import static edu.kit.kastel.vads.compiler.parser.type.BasicType.INT;
import static edu.kit.kastel.vads.compiler.parser.type.BasicType.VOID;

import edu.kit.kastel.vads.compiler.parser.ast.FunctionTree;
import edu.kit.kastel.vads.compiler.parser.ast.NameTree;
import edu.kit.kastel.vads.compiler.parser.ast.ProgramTree;
import edu.kit.kastel.vads.compiler.parser.ast.TypeTree;
import edu.kit.kastel.vads.compiler.parser.ast.expression.BooleanTree;
import edu.kit.kastel.vads.compiler.parser.ast.expression.ExpressionTree;
import edu.kit.kastel.vads.compiler.parser.ast.expression.IdentExpressionTree;
import edu.kit.kastel.vads.compiler.parser.ast.expression.LiteralTree;
import edu.kit.kastel.vads.compiler.parser.ast.expression.operation.BinaryOperationTree;
import edu.kit.kastel.vads.compiler.parser.ast.expression.operation.TernaryOperationTree;
import edu.kit.kastel.vads.compiler.parser.ast.expression.operation.UnaryOperationTree;
import edu.kit.kastel.vads.compiler.parser.ast.lvalue.LValueIdentTree;
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
import edu.kit.kastel.vads.compiler.parser.symbol.Name;
import edu.kit.kastel.vads.compiler.parser.type.Type;
import edu.kit.kastel.vads.compiler.parser.visitor.Visitor;
import edu.kit.kastel.vads.compiler.semantic.SemanticException;

public class TypedAnalysis implements Visitor<TypedContext, TypedTree<?>> {
    @Override
    public TypedTree<AssignmentTree> visit(AssignmentTree assignmentTree, TypedContext data) {
        var lValueTree = assignmentTree.lValue().accept(this, data);
        var type = assignmentTree.expression().accept(this, data);
        switch (assignmentTree.operator().type()) {
            case ASSIGN -> {
                if (lValueTree.type() != type.type()) {
                    throw new SemanticException(assignmentTree,
                        assignmentTree.operator().asString() + "assignment type mismatch: " +
                            lValueTree.type() +
                            " != " + type.type());
                }
            }
            case ASSIGN_DIV, ASSIGN_MINUS, ASSIGN_MOD, ASSIGN_MUL, ASSIGN_PLUS, ASSIGN_AND,
                 ASSIGN_OR, ASSIGN_XOR, ASSIGN_SHIFT_LEFT, ASSIGN_SHIFT_RIGHT -> {
                if (lValueTree.type() != INT || type.type() != INT) {
                    throw new SemanticException(assignmentTree,
                        assignmentTree.operator().asString() +
                            " assignment type mismatch: expected int, but got " +
                            lValueTree.type() +
                            " and " + type.type());
                }
            }
            default -> throw new SemanticException(assignmentTree,
                "Unsupported assignment operator: " + assignmentTree.operator().asString());
        }

        return new TypedTree<>(assignmentTree, lValueTree.type());
    }

    @Override
    public TypedTree<BinaryOperationTree> visit(BinaryOperationTree binaryOperationTree,
                                                TypedContext data) {
        var lhs = binaryOperationTree.lhs().accept(this, data);
        var rhs = binaryOperationTree.rhs().accept(this, data);
        var operator = binaryOperationTree.operatorType();

        switch (operator) {
            case EQUAL, NOT_EQUAL:
                if (lhs.type() != rhs.type()) {
                    throw new SemanticException(binaryOperationTree,
                        "Binary operation " + operator +
                            " requires both operands to be of the same type, but got " +
                            lhs.type() + " and " + rhs.type());
                }
                return new TypedTree<>(binaryOperationTree, BOOL);
            case PLUS, MINUS, MUL, DIV, MOD, SHIFT_LEFT, SHIFT_RIGHT, BITWISE_OR, BITWISE_AND,
                 BITWISE_XOR:
                if (lhs.type() != INT || rhs.type() != INT) {
                    throw new SemanticException(binaryOperationTree,
                        "Binary operation " + operator +
                            " requires both operands to be of type int, but got " +
                            lhs.type() + " and " + rhs.type());
                }
                return new TypedTree<>(binaryOperationTree, INT);
            case LESS, LESS_EQUAL, GREATER, GREATER_EQUAL:
                if (lhs.type() != INT || rhs.type() != INT) {
                    throw new SemanticException(binaryOperationTree,
                        "Binary operation " + operator +
                            " requires both operands to be of type int, but got " +
                            lhs.type() + " and " + rhs.type());
                }
                return new TypedTree<>(binaryOperationTree, BOOL);
            case AND, OR:
                if (lhs.type() != BOOL || rhs.type() != BOOL) {
                    throw new SemanticException(binaryOperationTree,
                        "Binary operation " + operator +
                            " requires both operands to be of type boolean, but got " +
                            lhs.type() + " and " + rhs.type());
                }
                return new TypedTree<>(binaryOperationTree, BOOL);
        }

        throw new SemanticException(binaryOperationTree,
            "Unknown binary operation: " + operator);
    }

    @Override
    public TypedTree<BlockTree> visit(BlockTree blockTree, TypedContext data) {
        data.push();

        try {
        ReturnTree returnTree = null;
        for (StatementTree statement : blockTree.statements()) {
            // First return statement found in the block
            if (returnTree == null && statement instanceof ReturnTree foundReturnTree) {
                returnTree = foundReturnTree;
            }
            statement.accept(this, data);
        }

        Type returnType = returnTree == null
            ? VOID
            : returnTree.accept(this, data).type();

        return new TypedTree<>(blockTree, returnType);
        } finally {
            data.pop();
        }
    }

    @Override
    public TypedTree<BooleanTree> visit(BooleanTree booleanTree, TypedContext data) {
        return new TypedTree<>(booleanTree, BOOL);
    }

    @Override
    public TypedTree<BreakTree> visit(BreakTree breakTree, TypedContext data) {
        return new TypedTree<>(breakTree, VOID);
    }

    @Override
    public TypedTree<ContinueTree> visit(ContinueTree continueTree, TypedContext data) {
        return new TypedTree<>(continueTree, VOID);
    }

    @Override
    public TypedTree<DeclarationTree> visit(DeclarationTree declarationTree, TypedContext data) {
        Type declaredType = declarationTree.type().type();
        Name name = declarationTree.name().name();
        data.addVariable(name, declaredType);

        ExpressionTree initializer = declarationTree.initializer();
        if (initializer != null) {
            var typedInit = initializer.accept(this, data);
            if (typedInit.type() != declaredType) {
                throw new SemanticException(declarationTree,
                    "Initializer type " + typedInit.type() + " does not match declared type " +
                        declaredType);
            }
        }

        return new TypedTree<>(declarationTree, declaredType);
    }

    @Override
    public TypedTree<ForTree> visit(ForTree forTree, TypedContext data) {
        data.push();

        try {
            var init = forTree.init();
            if (init != null) {
                init.accept(this, data);
            }

            var condition = forTree.condition().accept(this, data);
            if (condition.type() != BOOL) {
                throw new SemanticException(forTree,
                    "For loop condition must be of type boolean, but is " + condition.type());
            }

            var update = forTree.update();
            if (update != null) {
                update.accept(this, data);
            }

            return new TypedTree<>(forTree, VOID);
        } finally {
            data.pop();
        }
    }

    @Override
    public TypedTree<FunctionTree> visit(FunctionTree functionTree, TypedContext data) {
        var returnTypeTree = functionTree.returnType().accept(this, data);
        var bodyTree = functionTree.body().accept(this, data);

        if (returnTypeTree.type() != bodyTree.type()) {
            throw new SemanticException(functionTree,
                "Function return type " + returnTypeTree.type() +
                    " does not match body type " + bodyTree.type());
        }

        return new TypedTree<>(functionTree, returnTypeTree.type());
    }

    @Override
    public TypedTree<IdentExpressionTree> visit(IdentExpressionTree identExpressionTree, TypedContext data) {
        var identType = data.getVariableType(identExpressionTree.name().name());
        return new TypedTree<>(identExpressionTree, identType);
    }

    @Override
    public TypedTree<IfTree> visit(IfTree ifTree, TypedContext data) {
        var condition = ifTree.condition().accept(this, data);
        if (condition.type() != BOOL) {
            throw new SemanticException(ifTree,
                "If condition must be of type boolean, but is " + condition.type());
        }

        ifTree.thenBlock().accept(this, data);

        var elseBlock = ifTree.elseBlock();
        if (elseBlock != null) {
            elseBlock.accept(this, data);
        }

        return new TypedTree<>(ifTree, VOID);
    }

    @Override
    public TypedTree<LiteralTree> visit(LiteralTree literalTree, TypedContext data) {
        var _ = (int) literalTree.parseValue().orElseThrow();
        return new TypedTree<>(literalTree, INT);
    }

    @Override
    public TypedTree<LValueIdentTree> visit(LValueIdentTree lValueIdentTree, TypedContext data) {
        Type nameType = data.getVariableType(lValueIdentTree.name().name());
        return new TypedTree<>(lValueIdentTree, nameType);
    }

    @Override
    public TypedTree<NameTree> visit(NameTree nameTree, TypedContext data) {
        Type identType = data.tryGetVariableType(nameTree.name());
        return new TypedTree<>(nameTree, identType == null ? VOID : identType);
    }

    @Override
    public TypedTree<ProgramTree> visit(ProgramTree programTree, TypedContext data) {
        return new TypedTree<>(programTree, VOID);
    }

    @Override
    public TypedTree<ReturnTree> visit(ReturnTree returnTree, TypedContext data) {
        var expressionTree = returnTree.expression().accept(this, data);
        return new TypedTree<>(returnTree, expressionTree.type());
    }

    @Override
    public TypedTree<TernaryOperationTree> visit(TernaryOperationTree ternaryOperationTree, TypedContext data) {
        var condition = ternaryOperationTree.condition().accept(this, data);
        if (condition.type() != BOOL) {
            throw new SemanticException(
                "Ternary operation condition must be of type boolean, but is " +
                    condition.type());
        }

        var trueBranch = ternaryOperationTree.trueBranch().accept(this, data);
        var falseBranch = ternaryOperationTree.falseBranch().accept(this, data);

        if (trueBranch.type() != falseBranch.type()) {
            throw new SemanticException(
                "Ternary operation branches must have the same type, but got " +
                    trueBranch.type() + " and " + falseBranch.type());
        }

        return new TypedTree<>(ternaryOperationTree, trueBranch.type());
    }

    @Override
    public TypedTree<TypeTree> visit(TypeTree typeTree, TypedContext data) {
        return new TypedTree<>(typeTree, typeTree.type());
    }

    @Override
    public TypedTree<UnaryOperationTree> visit(UnaryOperationTree unaryOperationTree, TypedContext data) {
        var operand = unaryOperationTree.operand().accept(this, data);
        var operator = unaryOperationTree.operator();

        switch (operator.type()) {
            case NOT:
                if (operand.type() != BOOL) {
                    throw new SemanticException(
                        "Unary operation NOT requires operand of type boolean, but got " +
                            operand.type());
                }
                return new TypedTree<>(unaryOperationTree, BOOL);
            case BITWISE_NOT, MINUS:
                if (operand.type() != INT) {
                    throw new SemanticException("Unary operation " + operator.asString() +
                        " requires operand of type int, but got " + operand.type());
                }
                return new TypedTree<>(unaryOperationTree, INT);
            default:
                throw new SemanticException("Unknown unary operation: " + operator.asString());
        }
    }

    @Override
    public TypedTree<WhileTree> visit(WhileTree whileTree, TypedContext data) {
        var condition = whileTree.condition().accept(this, data);
        if (condition.type() != BOOL) {
            throw new SemanticException(whileTree,
                "While loop condition must be of type boolean, but is " + condition.type());
        }

        whileTree.body().accept(this, data);

        return new TypedTree<>(whileTree, VOID);
    }

}


