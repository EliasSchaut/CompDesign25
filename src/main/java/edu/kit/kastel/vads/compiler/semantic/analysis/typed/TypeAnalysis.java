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

/**
 * A visitor that performs type analysis on the AST.
 * It returns a {@link Type} for each tree, which is VOID when the tree
 * is not typed or doesn't have a consistent type in all control paths.
 */
public class TypeAnalysis implements Visitor<TypedContext, Type> {
    @Override
    public Type visit(AssignmentTree assignmentTree, TypedContext data) {
        Type lValueType;
        switch (assignmentTree.lValue()) {
            case LValueIdentTree(var name) ->
                lValueType = name.accept(this, data);
        }

        var assignedType = assignmentTree.expression().accept(this, data);
        switch (assignmentTree.operator().type()) {
            case ASSIGN -> {
                if (lValueType != assignedType) {
                    throw new SemanticException(assignmentTree,
                        assignmentTree.operator().asString() + "assignment type mismatch: " +
                            lValueType + " != " + assignedType);
                }
            }
            case ASSIGN_DIV, ASSIGN_MINUS, ASSIGN_MOD, ASSIGN_MUL, ASSIGN_PLUS, ASSIGN_AND,
                 ASSIGN_OR, ASSIGN_XOR, ASSIGN_SHIFT_LEFT, ASSIGN_SHIFT_RIGHT -> {
                if (lValueType != INT || assignedType != INT) {
                    throw new SemanticException(assignmentTree,
                        assignmentTree.operator().asString() +
                            " assignment type mismatch: expected int, but got " +
                            lValueType + " and " + assignedType);
                }
            }
            default -> throw new SemanticException(assignmentTree,
                "Unsupported assignment operator: " + assignmentTree.operator().asString());
        }

        return VOID;
    }

    @Override
    public Type visit(BinaryOperationTree binaryOperationTree,
                                                TypedContext data) {
        var lhs = binaryOperationTree.lhs().accept(this, data);
        var rhs = binaryOperationTree.rhs().accept(this, data);
        var operator = binaryOperationTree.operatorType();

        return switch (operator) {
            case EQUAL, NOT_EQUAL -> {
                if (lhs != rhs) {
                    throw new SemanticException(binaryOperationTree,
                        "Binary operation " + operator +
                            " requires both operands to be of the same type, but got " +
                            lhs + " and " + rhs);
                }
                yield BOOL;
            }
            case PLUS, MINUS, MUL, DIV, MOD, SHIFT_LEFT, SHIFT_RIGHT, BITWISE_OR, BITWISE_AND,
                 BITWISE_XOR -> {
                if (lhs != INT || rhs != INT) {
                    throw new SemanticException(binaryOperationTree,
                        "Binary operation " + operator +
                            " requires both operands to be of type int, but got " +
                            lhs + " and " + rhs);
                }
                yield INT;
            }
            case LESS, LESS_EQUAL, GREATER, GREATER_EQUAL -> {
                if (lhs != INT || rhs != INT) {
                    throw new SemanticException(binaryOperationTree,
                        "Binary operation " + operator +
                            " requires both operands to be of type int, but got " +
                            lhs + " and " + rhs);
                }
                yield BOOL;
            }
            case AND, OR -> {
                if (lhs != BOOL || rhs != BOOL) {
                    throw new SemanticException(binaryOperationTree,
                        "Binary operation " + operator +
                            " requires both operands to be of type boolean, but got " +
                            lhs + " and " + rhs);
                }
                yield BOOL;
            }
            default -> throw new SemanticException(binaryOperationTree,
                "Unknown binary operation: " + operator);
        };

    }

    @Override
    public Type visit(BlockTree blockTree, TypedContext data) {
        data.push();

        try {
            Type returnType = null;
            for (StatementTree statement : blockTree.statements()) {
                var statementType = statement.accept(this, data);
                if (statement instanceof BreakTree || statement instanceof ContinueTree) {
                    // Break and continue statements do not affect the return type
                    break;
                }

                if (statementType != VOID) {
                    if (returnType == null) {
                        returnType = statementType;
                    } else if (returnType != statementType) {
                        throw new SemanticException(statement,
                            "Inconsistent return types in block: " + returnType +
                                " and " + statementType);
                    }
                }
            }

            return returnType == null
                ? VOID
                : returnType;
        } finally {
            data.pop();
        }
    }

    @Override
    public Type visit(BooleanTree booleanTree, TypedContext data) {
        return BOOL;
    }

    @Override
    public Type visit(BreakTree breakTree, TypedContext data) {
        return VOID;
    }

    @Override
    public Type visit(ContinueTree continueTree, TypedContext data) {
        return VOID;
    }

    @Override
    public Type visit(DeclarationTree declarationTree, TypedContext data) {
        Type declaredType = declarationTree.type().type();
        Name name = declarationTree.name().name();
        data.addVariable(name, declaredType);

        ExpressionTree initializer = declarationTree.initializer();
        if (initializer != null) {
            var initType = initializer.accept(this, data);
            if (initType != declaredType) {
                throw new SemanticException(declarationTree,
                    "Initializer type " + initType + " does not match declared type " +
                        declaredType);
            }
        }

        return VOID;
    }

    @Override
    public Type visit(ForTree forTree, TypedContext data) {
        data.push();

        try {
            var init = forTree.init();
            if (init != null) {
                init.accept(this, data);
            }

            var conditionType = forTree.condition().accept(this, data);
            if (conditionType != BOOL) {
                throw new SemanticException(forTree,
                    "For loop condition must be of type boolean, but is " + conditionType);
            }

            var update = forTree.update();
            if (update != null) {
                update.accept(this, data);
            }

            return forTree.body().accept(this, data);
        } finally {
            data.pop();
        }
    }

    @Override
    public Type visit(FunctionTree functionTree, TypedContext data) {
        var returnType = functionTree.returnType().type();
        var bodyType = functionTree.body().accept(this, data);

        if (returnType != bodyType) {
            throw new SemanticException(functionTree,
                "Function return type " + returnType +
                    " does not match body type " + bodyType);
        }

        return returnType;
    }

    @Override
    public Type visit(IdentExpressionTree identExpressionTree, TypedContext data) {
        return data.getVariableType(identExpressionTree.name().name());
    }

    @Override
    public Type visit(IfTree ifTree, TypedContext data) {
        var conditionType = ifTree.condition().accept(this, data);
        if (conditionType != BOOL) {
            throw new SemanticException(ifTree,
                "If condition must be of type boolean, but is " + conditionType);
        }

        var thenType = ifTree.thenBlock().accept(this, data);

        var elseBlock = ifTree.elseBlock();
        if (elseBlock == null) {
            // No return type on all control paths
            return VOID;
        }

        var elseType = elseBlock.accept(this, data);
        if (thenType != VOID && elseType != VOID) {
            if (thenType == elseType) {
                return thenType;
            }

            throw new SemanticException(ifTree,
                "If branches must have the same type, but got " +
                    thenType + " and " + elseBlock.accept(this, data));
        }

        return VOID;
    }

    @Override
    public Type visit(LiteralTree literalTree, TypedContext data) {
        var _ = (int) literalTree.parseValue().orElseThrow();
        return INT;
    }

    @Override
    public Type visit(LValueIdentTree lValueIdentTree, TypedContext data) {
        return VOID;
    }

    @Override
    public Type visit(NameTree nameTree, TypedContext data) {
        Type identType = data.tryGetVariableType(nameTree.name());
        return identType == null ? VOID : identType;
    }

    @Override
    public Type visit(ProgramTree programTree, TypedContext data) {
        return VOID;
    }

    @Override
    public Type visit(ReturnTree returnTree, TypedContext data) {
        return returnTree.expression().accept(this, data);
    }

    @Override
    public Type visit(TernaryOperationTree ternaryOperationTree, TypedContext data) {
        var conditionType = ternaryOperationTree.condition().accept(this, data);
        if (conditionType != BOOL) {
            throw new SemanticException(
                "Ternary operation condition must be of type boolean, but is " +
                    conditionType);
        }

        var trueBranchType = ternaryOperationTree.trueBranch().accept(this, data);
        var falseBranchType = ternaryOperationTree.falseBranch().accept(this, data);

        if (trueBranchType != falseBranchType) {
            throw new SemanticException(
                "Ternary operation branches must have the same type, but got " +
                    trueBranchType + " and " + falseBranchType);
        }

        return trueBranchType;
    }

    @Override
    public Type visit(TypeTree typeTree, TypedContext data) {
        return VOID;
    }

    @Override
    public Type visit(UnaryOperationTree unaryOperationTree, TypedContext data) {
        var operandType = unaryOperationTree.operand().accept(this, data);
        var operator = unaryOperationTree.operator();

        return switch (operator.type()) {
            case NOT -> {
                if (operandType != BOOL) {
                    throw new SemanticException(
                        "Unary operation NOT requires operand of type boolean, but got " +
                            operandType);
                }
                yield BOOL;
            }
            case BITWISE_NOT, MINUS -> {
                if (operandType != INT) {
                    throw new SemanticException("Unary operation " + operator.asString() +
                        " requires operand of type int, but got " + operandType);
                }
                yield INT;
            }
            default ->
                throw new SemanticException("Unknown unary operation: " + operator.asString());
        };
    }

    @Override
    public Type visit(WhileTree whileTree, TypedContext data) {
        var conditionType = whileTree.condition().accept(this, data);
        if (conditionType != BOOL) {
            throw new SemanticException(whileTree,
                "While loop condition must be of type boolean, but is " + conditionType);
        }

        return whileTree.body().accept(this, data);
    }

}


