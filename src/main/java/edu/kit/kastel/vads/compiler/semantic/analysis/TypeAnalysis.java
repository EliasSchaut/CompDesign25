package edu.kit.kastel.vads.compiler.semantic.analysis;

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
import edu.kit.kastel.vads.compiler.parser.visitor.Context;
import edu.kit.kastel.vads.compiler.parser.visitor.ScopedContext;
import edu.kit.kastel.vads.compiler.parser.visitor.Visitor;
import edu.kit.kastel.vads.compiler.semantic.SemanticException;
import java.util.HashMap;
import org.jspecify.annotations.Nullable;

/**
 * A visitor that performs type analysis on the AST.
 * It returns a {@link Type} for each tree, which is VOID when the tree
 * is not typed or doesn't have a consistent type in all control paths.
 */
public class TypeAnalysis implements Visitor<ScopedContext<TypeAnalysis.TypeContext>, Type> {
    public record TypeContext(HashMap<Name, Type> variableInfo) implements Context<TypeContext> {
        public TypeContext() {
            this(new HashMap<>());
        }

        public TypeContext(TypeContext other) {
            this(new HashMap<>(other.variableInfo));
        }

        @Override
        public TypeContext copy() {
            return new TypeContext(this);
        }

        public void addVariable(Name name, Type type) {
            if (variableInfo.containsKey(name)) {
                throw new SemanticException("Variable " + name + " is already declared in context");
            }
            variableInfo.put(name, type);
        }

        public Type getVariableType(Name name) {
            var type = tryGetVariableType(name);
            if (type == null) {
                throw new SemanticException("Variable " + name + " is not declared in context");
            }
            return type;
        }

        public @Nullable Type tryGetVariableType(Name name) {
            return variableInfo.get(name);
        }
    }

    @Override
    public Type visit(AssignmentTree assignmentTree, ScopedContext<TypeAnalysis.TypeContext> scope) {
        Type lValueType;
        switch (assignmentTree.lValue()) {
            case LValueIdentTree(var name) -> lValueType = visit(name, scope);
        }

        var assignedType = visit(assignmentTree.expression(), scope);
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
                      ScopedContext<TypeAnalysis.TypeContext> scope) {
        var lhs = visit(binaryOperationTree.lhs(), scope);
        var rhs = visit(binaryOperationTree.rhs(), scope);
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
    public Type visit(BlockTree blockTree, ScopedContext<TypeAnalysis.TypeContext> scope) {
        scope.push();

        try {
            Type returnType = null;
            for (StatementTree statement : blockTree.statements()) {
                var statementType = visit(statement, scope);

                if (statementType != VOID) {
                    if (returnType == null) {
                        returnType = statementType;
                    } else if (returnType != statementType) {
                        throw new SemanticException(statement,
                            "Inconsistent return types in block: " + returnType +
                                " and " + statementType);
                    }
                }

                if (statement instanceof BreakTree ||
                    statement instanceof ContinueTree ||
                    statement instanceof ReturnTree) {
                    // Here the block ends
                    break;
                }
            }

            return returnType == null
                ? VOID
                : returnType;
        } finally {
            scope.pop();
        }
    }

    @Override
    public Type visit(BooleanTree booleanTree, ScopedContext<TypeAnalysis.TypeContext> scope) {
        return BOOL;
    }

    @Override
    public Type visit(BreakTree breakTree, ScopedContext<TypeAnalysis.TypeContext> scope) {
        return VOID;
    }

    @Override
    public Type visit(ContinueTree continueTree, ScopedContext<TypeAnalysis.TypeContext> scope) {
        return VOID;
    }

    @Override
    public Type visit(DeclarationTree declarationTree, ScopedContext<TypeAnalysis.TypeContext> scope) {
        Type declaredType = declarationTree.type().type();
        Name name = declarationTree.name().name();
        data.addVariable(name, declaredType);

        ExpressionTree initializer = declarationTree.initializer();
        if (initializer != null) {
            var initType = visit(initializer, scope);
            if (initType != declaredType) {
                throw new SemanticException(declarationTree,
                    "Initializer type " + initType + " does not match declared type " +
                        declaredType);
            }
        }

        return VOID;
    }

    @Override
    public Type visit(ForTree forTree, ScopedContext<TypeAnalysis.TypeContext> scope) {
        scope.push();

        try {
            var init = forTree.init();
            if (init != null) {
                visit(init, scope);
            }

            var conditionType = visit(forTree.condition(), scope);
            if (conditionType != BOOL) {
                throw new SemanticException(forTree,
                    "For loop condition must be of type boolean, but is " + conditionType);
            }

            var bodyType = visit(forTree.body(), scope);

            var update = forTree.update();
            if (update != null) {
                visit(update, scope);
            }

            return bodyType;
        } finally {
            scope.pop();
        }
    }

    @Override
    public Type visit(FunctionTree functionTree, ScopedContext<TypeAnalysis.TypeContext> scope) {
        var returnType = functionTree.returnType().type();
        var bodyType = visit(functionTree.body(), scope);

        if (returnType != bodyType) {
            throw new SemanticException(functionTree,
                "Function return type " + returnType +
                    " does not match body type " + bodyType);
        }

        return returnType;
    }

    @Override
    public Type visit(IdentExpressionTree identExpressionTree, ScopedContext<TypeAnalysis.TypeContext> scope) {
        return scope.get().getVariableType(identExpressionTree.name().name());
    }

    @Override
    public Type visit(IfTree ifTree, ScopedContext<TypeAnalysis.TypeContext> scope) {
        var conditionType = visit(ifTree.condition(), scope);
        if (conditionType != BOOL) {
            throw new SemanticException(ifTree,
                "If condition must be of type boolean, but is " + conditionType);
        }

        var thenType = visit(ifTree.thenBlock(), scope);

        var elseBlock = ifTree.elseBlock();
        if (elseBlock == null) {
            // No return type on all control paths
            return VOID;
        }

        var elseType = visit(elseBlock, scope);
        if (thenType != VOID && elseType != VOID) {
            if (thenType == elseType) {
                return thenType;
            }

            throw new SemanticException(ifTree,
                "If branches must have the same type, but got " +
                    thenType + " and " + visit(elseBlock, scope));
        }

        return VOID;
    }

    @Override
    public Type visit(LiteralTree literalTree, ScopedContext<TypeAnalysis.TypeContext> scope) {
        var _ = (int) literalTree.parseValue().orElseThrow();
        return INT;
    }

    @Override
    public Type visit(LValueIdentTree lValueIdentTree, ScopedContext<TypeAnalysis.TypeContext> scope) {
        return VOID;
    }

    @Override
    public Type visit(NameTree nameTree, ScopedContext<TypeAnalysis.TypeContext> scope) {
        Type identType = scope.get().tryGetVariableType(nameTree.name());
        return identType == null ? VOID : identType;
    }

    @Override
    public Type visit(ProgramTree programTree, ScopedContext<TypeAnalysis.TypeContext> scope) {
        for (FunctionTree topLevelTree : programTree.topLevelTrees()) {
            visit(topLevelTree, scope);
        }
        return VOID;
    }

    @Override
    public Type visit(ReturnTree returnTree, ScopedContext<TypeAnalysis.TypeContext> scope) {
        return visit(returnTree.expression(), scope);
    }

    @Override
    public Type visit(TernaryOperationTree ternaryOperationTree, ScopedContext<TypeAnalysis.TypeContext> scope) {
        var conditionType = visit(ternaryOperationTree.condition(), scope);
        if (conditionType != BOOL) {
            throw new SemanticException(
                "Ternary operation condition must be of type boolean, but is " +
                    conditionType);
        }

        var trueBranchType = visit(ternaryOperationTree.trueBranch(), scope);
        var falseBranchType = visit(ternaryOperationTree.falseBranch(), scope);

        if (trueBranchType != falseBranchType) {
            throw new SemanticException(
                "Ternary operation branches must have the same type, but got " +
                    trueBranchType + " and " + falseBranchType);
        }

        return trueBranchType;
    }

    @Override
    public Type visit(TypeTree typeTree, ScopedContext<TypeAnalysis.TypeContext> scope) {
        return VOID;
    }

    @Override
    public Type visit(UnaryOperationTree unaryOperationTree, ScopedContext<TypeAnalysis.TypeContext> scope) {
        var operandType = visit(unaryOperationTree.operand(), scope);
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
    public Type visit(WhileTree whileTree, ScopedContext<TypeAnalysis.TypeContext> scope) {
        var conditionType = visit(whileTree.condition(), scope);
        if (conditionType != BOOL) {
            throw new SemanticException(whileTree,
                "While loop condition must be of type boolean, but is " + conditionType);
        }

        return visit(whileTree.body(), scope);
    }
}
