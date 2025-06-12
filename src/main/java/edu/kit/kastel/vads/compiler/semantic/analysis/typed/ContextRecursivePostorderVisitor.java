package edu.kit.kastel.vads.compiler.semantic.analysis.typed;

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
import edu.kit.kastel.vads.compiler.parser.visitor.Visitor;

/// A visitor that traverses a tree in postorder
/// @param <R> a type for a return type
public class ContextRecursivePostorderVisitor<R> implements Visitor<TypedContext, R> {
    private final Visitor<TypedContext, R> visitor;

    public ContextRecursivePostorderVisitor(Visitor<TypedContext, R> visitor) {
        this.visitor = visitor;
    }

    @Override
    public R visit(AssignmentTree assignmentTree, TypedContext data) {
        R r = assignmentTree.lValue().accept(this, data);
        r = assignmentTree.expression().accept(this, accumulate(data, r));
        r = this.visitor.visit(assignmentTree, accumulate(data, r));
        return r;
    }

    @Override
    public R visit(BinaryOperationTree binaryOperationTree, TypedContext data) {
        R r = binaryOperationTree.lhs().accept(this, data);
        r = binaryOperationTree.rhs().accept(this, accumulate(data, r));
        r = this.visitor.visit(binaryOperationTree, accumulate(data, r));
        return r;
    }

    @Override
    public R visit(BlockTree blockTree, TypedContext data) {
        R r;

        data.push();
        for (StatementTree statement : blockTree.statements()) {
            r = statement.accept(this, data);
            data = accumulate(data, r);
        }
        data.pop();

        r = this.visitor.visit(blockTree, data);

        return r;
    }

    @Override
    public R visit(BooleanTree booleanTree, TypedContext data) {
        return this.visitor.visit(booleanTree, data);
    }

    @Override
    public R visit(BreakTree breakTree, TypedContext data) {
        return this.visitor.visit(breakTree, data);
    }

    @Override
    public R visit(ContinueTree continueTree, TypedContext data) {
        return this.visitor.visit(continueTree, data);
    }

    @Override
    public R visit(DeclarationTree declarationTree, TypedContext data) {
        R r = declarationTree.type().accept(this, data);
        r = declarationTree.name().accept(this, accumulate(data, r));
        ExpressionTree declaration = declarationTree.initializer();
        if (declaration != null) {
            r = declaration.accept(this, accumulate(data, r));
        }
        r = this.visitor.visit(declarationTree, accumulate(data, r));
        return r;
    }

    @Override
    public R visit(ForTree forTree, TypedContext data) {
        R r = null;

        data.push();
        StatementTree init = forTree.init();
        if (init != null) r = init.accept(this, data);
        r = forTree.condition().accept(this, r == null ? data : accumulate(data, r));
        StatementTree update = forTree.update();
        if (update != null) r = update.accept(this, accumulate(data, r));
        r = forTree.body().accept(this, accumulate(data, r));
        data.pop();

        r = this.visitor.visit(forTree, accumulate(data, r));
        return r;
    }

    @Override
    public R visit(FunctionTree functionTree, TypedContext data) {
        R r = functionTree.returnType().accept(this, data);
        r = functionTree.name().accept(this, accumulate(data, r));
        r = functionTree.body().accept(this, accumulate(data, r));
        r = this.visitor.visit(functionTree, accumulate(data, r));
        return r;
    }

    @Override
    public R visit(IdentExpressionTree identExpressionTree, TypedContext data) {
        R r = identExpressionTree.name().accept(this, data);
        r = this.visitor.visit(identExpressionTree, accumulate(data, r));
        return r;
    }

    @Override
    public R visit(IfTree ifTree, TypedContext data) {
        R r = ifTree.condition().accept(this, data);
        r = ifTree.thenBlock().accept(this, accumulate(data, r));
        StatementTree elseBlock = ifTree.elseBlock();
        if (elseBlock != null) {
            r = elseBlock.accept(this, accumulate(data, r));
        }
        r = this.visitor.visit(ifTree, accumulate(data, r));
        return r;
    }

    @Override
    public R visit(LiteralTree literalTree, TypedContext data) {
        return this.visitor.visit(literalTree, data);
    }

    @Override
    public R visit(LValueIdentTree lValueIdentTree, TypedContext data) {
        R r = lValueIdentTree.name().accept(this, data);
        r = this.visitor.visit(lValueIdentTree, accumulate(data, r));
        return r;
    }

    @Override
    public R visit(NameTree nameTree, TypedContext data) {
        return this.visitor.visit(nameTree, data);
    }

    @Override
    public R visit(ProgramTree programTree, TypedContext data) {
        R r;
        TypedContext d = data;
        for (FunctionTree tree : programTree.topLevelTrees()) {
            r = tree.accept(this, d);
            d = accumulate(data, r);
        }
        r = this.visitor.visit(programTree, d);
        return r;
    }

    @Override
    public R visit(ReturnTree returnTree, TypedContext data) {
        R r = returnTree.expression().accept(this, data);
        r = this.visitor.visit(returnTree, accumulate(data, r));
        return r;
    }

    @Override
    public R visit(TernaryOperationTree ternaryOperationTree, TypedContext data) {
        R r = ternaryOperationTree.condition().accept(this, data);
        r = ternaryOperationTree.trueBranch().accept(this, accumulate(data, r));
        r = ternaryOperationTree.falseBranch().accept(this, accumulate(data, r));
        r = this.visitor.visit(ternaryOperationTree, accumulate(data, r));
        return r;
    }

    @Override
    public R visit(TypeTree typeTree, TypedContext data) {
        return this.visitor.visit(typeTree, data);
    }

    @Override
    public R visit(UnaryOperationTree unaryOperationTree, TypedContext data) {
        R r = unaryOperationTree.operand().accept(this, data);
        r = this.visitor.visit(unaryOperationTree, accumulate(data, r));
        return r;
    }

    @Override
    public R visit(WhileTree whileTree, TypedContext data) {
        R r = whileTree.condition().accept(this, data);
        r = whileTree.body().accept(this, accumulate(data, r));
        r = this.visitor.visit(whileTree, accumulate(data, r));
        return r;
    }

    protected TypedContext accumulate(TypedContext data, R value) {
        return data;
    }
}
