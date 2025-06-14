package edu.kit.kastel.vads.compiler.semantic.analysis;

import edu.kit.kastel.vads.compiler.lexer.tokens.Operator;
import edu.kit.kastel.vads.compiler.parser.ast.FunctionTree;
import edu.kit.kastel.vads.compiler.parser.ast.ProgramTree;
import edu.kit.kastel.vads.compiler.parser.ast.TypeTree;
import edu.kit.kastel.vads.compiler.parser.ast.expression.BooleanTree;
import edu.kit.kastel.vads.compiler.parser.ast.expression.ExpressionTree;
import edu.kit.kastel.vads.compiler.parser.ast.expression.LiteralTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.AssignmentTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.BlockTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.DeclarationTree;
import edu.kit.kastel.vads.compiler.parser.ast.expression.IdentExpressionTree;
import edu.kit.kastel.vads.compiler.parser.ast.lvalue.LValueIdentTree;
import edu.kit.kastel.vads.compiler.parser.ast.NameTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.StatementTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.control.BreakTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.control.ContinueTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.control.ForTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.control.IfTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.control.WhileTree;
import edu.kit.kastel.vads.compiler.parser.symbol.Name;
import edu.kit.kastel.vads.compiler.parser.visitor.AggregateVisitor;
import edu.kit.kastel.vads.compiler.parser.visitor.ScopedContext;
import edu.kit.kastel.vads.compiler.semantic.Namespace;
import edu.kit.kastel.vads.compiler.semantic.SemanticException;
import org.jspecify.annotations.Nullable;
import java.util.List;
import java.util.Locale;

/// Checks that variables are
/// - declared before assignment
/// - not declared twice
/// - not initialized twice
/// - assigned before referenced
class VariableStatusAnalysis implements AggregateVisitor<ScopedContext<Namespace<VariableStatusAnalysis.VariableStatus>>, Namespace<VariableStatusAnalysis.VariableStatus>> {

    private final ReturnAnalysis returnAnalysis = new ReturnAnalysis(true);

    @Override
    public Namespace<VariableStatusAnalysis.VariableStatus> visit(AssignmentTree assignmentTree, ScopedContext<Namespace<VariableStatus>> scope) {
        var newNamespace = scope.get().copy();

        // Ensure that the expression is declared and initialized
        visit(assignmentTree.expression(), scope);

        switch (assignmentTree.lValue()) {
            case LValueIdentTree(var name) -> {
                VariableStatus status = newNamespace.get(name.name());
                if (assignmentTree.operator().type() == Operator.OperatorType.ASSIGN) {
                    checkDeclared(name, status);
                } else {
                    // Other assign operators (e.g., +=, -=) need the variable to be initialized already
                    checkInitialized(name, status);
                }

                if (status != VariableStatus.INITIALIZED) {
                    // only update when needed, reassignment is totally fine
                    updateStatus(newNamespace, VariableStatus.INITIALIZED, name.name());
                }
            }
        }

        return newNamespace;
    }

    @Override
    public Namespace<VariableStatusAnalysis.VariableStatus> visit(BlockTree blockTree, ScopedContext<Namespace<VariableStatus>> scope) {
        scope.push();
        var hasReturned = false;
        var namespaceBeforeReturned = scope.get();
        for (StatementTree statement : blockTree.statements()) {
            if (hasReturned) {
                // Continue to check malformed blocks after returns - ignore all other exceptions
                try {
                    var newNames = visit(statement, scope);
                    aggregate(scope, newNames);
                } catch (VariableStatusException e) {
                    // Ignore semantic exceptions of statements after returns
                    switch (e.expectedStatus) {
                        // Always throw if the variable was never declared
                        case DECLARED -> throw e;
                        // Always throw if variable is redeclared
                        case null -> throw e;
                        case INITIALIZED -> {
                            // Only throw when the variable was not declared before the return
                            if (namespaceBeforeReturned.get(e.variable) == null) {
                                throw e;
                            }
                        }
                    }
                }
            } else {
                var newNames = visit(statement, scope);
                aggregate(scope, newNames);

                var statementReturns = this.returnAnalysis.visit(statement, new ReturnAnalysis.ReturnState());
                if (statementReturns.returns) {
                    hasReturned = true;
                    namespaceBeforeReturned = scope.get().copy();
                }
            }
        }
        var blockNamespace = scope.get().copy();
        scope.pop();

        var outerScopeNamespace = scope.get();

        for (Name name : blockNamespace.getAllWhere(status -> status == VariableStatus.INITIALIZED)) {
            // Only update the status if the variable was already declared
            if (outerScopeNamespace.get(name) != null) {
                outerScopeNamespace.put(name, VariableStatus.INITIALIZED);
            }
        }

        return outerScopeNamespace;
    }

    @Override
    public Namespace<VariableStatusAnalysis.VariableStatus> visit(IfTree ifTree, ScopedContext<Namespace<VariableStatus>> scope) {
        var ifNamespace = scope.get();

        StatementTree thenBlock = ifTree.thenBlock();
        StatementTree elseBlock = ifTree.elseBlock();
        if (elseBlock != null) {
            var thenNamespace = this.visit(thenBlock, scope);
            List<Name> thenNames = thenNamespace.getAllWhere(status -> status == VariableStatus.INITIALIZED);
            var thenReturns = this.returnAnalysis.visit(thenBlock, new ReturnAnalysis.ReturnState());

            var elseNamespace = this.visit(elseBlock, scope);
            List<Name> elseNames = elseNamespace.getAllWhere(status -> status == VariableStatus.INITIALIZED);
            var elseReturns = this.returnAnalysis.visit(elseBlock, new ReturnAnalysis.ReturnState());

            if (thenReturns.returns) {
                if (!elseReturns.returns) {
                    for (Name name : elseNames) {
                        ifNamespace.put(name, VariableStatus.INITIALIZED);
                    }
                }
            } else {
                if (elseReturns.returns) {
                    for (Name name : thenNames) {
                        ifNamespace.put(name, VariableStatus.INITIALIZED);
                    }
                } else {
                    List<Name> sharedNames = thenNames
                        .stream()
                        .filter(elseNames::contains)
                        .toList();

                    for (Name sharedName : sharedNames) {
                        ifNamespace.put(sharedName, VariableStatus.INITIALIZED);
                    }
                }
            }
        } else {
            // Check then block without contributing to the namespace
            scope.push();
            this.visit(thenBlock, scope);
            scope.pop();
        }

        return ifNamespace;
    }

    @Override
    public Namespace<VariableStatus> visit(ProgramTree programTree,
                                           ScopedContext<Namespace<VariableStatus>> data) {
        for (FunctionTree functionTree : programTree.topLevelTrees()) {
            var functionNamespace = this.visit(functionTree, data);
            List<Name> initializedNames = functionNamespace.getAllWhere(status -> status == VariableStatus.INITIALIZED);
            for (Name name : initializedNames) {
                data.get().put(name, VariableStatus.INITIALIZED);
            }
        }

        return data.get();
    }

    @Override
    public Namespace<VariableStatus> visit(ForTree forTree,
                                           ScopedContext<Namespace<VariableStatus>> data) {
        // Do recursive check of the for loop
        data.push();
        AggregateVisitor.super.visit(forTree, data);
        data.pop();

        // For cannot contribute to the variable status - it might not ever run
        // The initializer will always run though, so it can contribute variable initialization
        StatementTree init = forTree.init();
        var namespace = data.get();
        if (init != null) {
            var initNamespace = visit(init, data);
            for (Name name : initNamespace.getAllWhere(status -> status == VariableStatus.INITIALIZED)) {
                // Only update the status if the variable was already declared
                if (namespace.get(name) != null) {
                    namespace.put(name, VariableStatus.INITIALIZED);
                }
            }
        }
        return namespace;
    }

    @Override
    public Namespace<VariableStatus> visit(WhileTree whileTree,
                                           ScopedContext<Namespace<VariableStatus>> data) {
        data.push();
        AggregateVisitor.super.visit(whileTree, data);
        data.pop();

        // While cannot contribute to the variable status - it might not ever run
        return data.get();
    }

    @Override
    public ScopedContext<Namespace<VariableStatus>> aggregate(
        ScopedContext<Namespace<VariableStatus>> data, Namespace<VariableStatus> value) {
        var namespace = data.get();
        for (Name name : value.names()) {
            VariableStatus status = value.get(name);
            if (status == VariableStatus.INITIALIZED) {
                namespace.put(name, VariableStatus.INITIALIZED);
            } else {
                namespace.put(name, VariableStatus.DECLARED);
            }
        }
        return data;
    }

    @Override
    public Namespace<VariableStatus> defaultData() {
        return new Namespace<>();
    }

    @Override
    public Namespace<VariableStatusAnalysis.VariableStatus> visit(DeclarationTree declarationTree, ScopedContext<Namespace<VariableStatus>> scope) {
        var newNamespace = scope.get().copy();

        ExpressionTree init = declarationTree.initializer();
        if (init != null) {
            // Ensure that the initializer has all variables properly declared and initialized
            visit(init, scope);
        }

        checkUndeclared(declarationTree.name(), newNamespace.get(declarationTree.name().name()));
        VariableStatus status = init == null
            ? VariableStatus.DECLARED
            : VariableStatus.INITIALIZED;
        updateStatus(newNamespace, status, declarationTree.name().name());
        return newNamespace;
    }

    @Override
    public Namespace<VariableStatusAnalysis.VariableStatus> visit(IdentExpressionTree identExpressionTree, ScopedContext<Namespace<VariableStatus>> scope) {
        var namespace = scope.get();
        VariableStatus status = namespace.get(identExpressionTree.name().name());
        checkInitialized(identExpressionTree.name(), status);
        return namespace;
    }

    @Override
    public Namespace<VariableStatus> visit(BooleanTree booleanTree,
                                           ScopedContext<Namespace<VariableStatus>> data) {
        return data.get();
    }

    @Override
    public Namespace<VariableStatus> visit(BreakTree breakTree,
                                           ScopedContext<Namespace<VariableStatus>> data) {
        return data.get();
    }

    @Override
    public Namespace<VariableStatus> visit(ContinueTree continueTree,
                                           ScopedContext<Namespace<VariableStatus>> data) {
        return data.get();
    }

    @Override
    public Namespace<VariableStatus> visit(LiteralTree literalTree,
                                           ScopedContext<Namespace<VariableStatus>> data) {
        return data.get();
    }

    @Override
    public Namespace<VariableStatus> visit(NameTree nameTree,
                                           ScopedContext<Namespace<VariableStatus>> data) {
        return data.get();
    }

    @Override
    public Namespace<VariableStatus> visit(TypeTree typeTree,
                                           ScopedContext<Namespace<VariableStatus>> data) {
        return data.get();
    }

    private static void checkDeclared(NameTree name, @Nullable VariableStatus status) {
        if (status == null) {
            throw new VariableStatusException(name.name(), VariableStatus.DECLARED, "Variable " + name + " must be declared before assignment");
        }
    }

    private static void checkInitialized(NameTree name, @Nullable VariableStatus status) {
        if (status == null || status == VariableStatus.DECLARED) {
            throw new VariableStatusException(name.name(), VariableStatus.INITIALIZED, "Variable " + name + " must be initialized before use");
        }
    }

    private static void checkUndeclared(NameTree name, @Nullable VariableStatus status) {
        if (status != null) {
            throw new VariableStatusException(name.name(), null, "Variable " + name + " is already declared");
        }
    }

    private static void updateStatus(Namespace<VariableStatus> namespace, VariableStatus status, Name name) {
        namespace.put(name, status, (existing, replacement) -> {
            if (existing.ordinal() >= replacement.ordinal()) {
                throw new VariableStatusException(name, status, "variable is already " + existing + ". Cannot be " + replacement + " here.");
            }
            return replacement;
        });
    }

    enum VariableStatus {
        DECLARED,
        INITIALIZED;

        @Override
        public String toString() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    static class VariableStatusException extends SemanticException {
        private final Name variable;
        private final @Nullable VariableStatus expectedStatus;

        public VariableStatusException(Name variable, VariableStatusAnalysis.@Nullable VariableStatus expectedStatus, String message) {
            super(message);
            this.variable = variable;
            this.expectedStatus = expectedStatus;
        }
    }
}
