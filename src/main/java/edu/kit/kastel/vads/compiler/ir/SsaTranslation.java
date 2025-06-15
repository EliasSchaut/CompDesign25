package edu.kit.kastel.vads.compiler.ir;

import edu.kit.kastel.vads.compiler.ir.node.*;
import edu.kit.kastel.vads.compiler.ir.node.binary.DivNode;
import edu.kit.kastel.vads.compiler.ir.node.binary.ModNode;
import edu.kit.kastel.vads.compiler.ir.node.block.Block;
import edu.kit.kastel.vads.compiler.ir.optimize.Optimizer;
import edu.kit.kastel.vads.compiler.ir.util.DebugInfo;
import edu.kit.kastel.vads.compiler.ir.util.DebugInfoHelper;
import edu.kit.kastel.vads.compiler.parser.ast.expression.BooleanTree;
import edu.kit.kastel.vads.compiler.parser.ast.expression.ExpressionTree;
import edu.kit.kastel.vads.compiler.parser.ast.expression.operation.UnaryOperationTree;
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
import edu.kit.kastel.vads.compiler.parser.ast.statement.control.BreakTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.control.ContinueTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.control.ForTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.control.IfTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.control.ReturnTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.StatementTree;
import edu.kit.kastel.vads.compiler.parser.ast.Tree;
import edu.kit.kastel.vads.compiler.parser.ast.TypeTree;
import edu.kit.kastel.vads.compiler.parser.ast.expression.operation.TernaryOperationTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.control.WhileTree;
import edu.kit.kastel.vads.compiler.parser.symbol.Name;
import edu.kit.kastel.vads.compiler.parser.visitor.Visitor;

import edu.kit.kastel.vads.compiler.semantic.analysis.SemanticAnalysis;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import java.util.function.BinaryOperator;

/// SSA translation as described in
/// [`Simple and Efficient Construction of Static Single Assignment Form`](https://compilers.cs.uni-saarland.de/papers/bbhlmz13cc.pdf).
///
/// This implementation also tracks side effect edges that can be used to avoid reordering of operations that cannot be
/// reordered.
///
/// We recommend to read the paper to better understand the mechanics implemented here.
public class SsaTranslation {
    private final FunctionTree function;
    private final GraphConstructor constructor;

    public SsaTranslation(FunctionTree function, Optimizer optimizer) {
        this.function = function;
        this.constructor = new GraphConstructor(optimizer, function.name().name().asString());
    }

    public IrGraph translate() {
        var visitor = new SsaTranslationVisitor();
        this.function.accept(visitor, this);
        return this.constructor.graph();
    }

    private void writeVariable(Name variable, Block block, Node value) {
        this.constructor.writeVariable(variable, block, value);
    }

    private Node readVariable(Name variable, Block block) {
        return this.constructor.readVariable(variable, block);
    }

    private Block currentBlock() {
        return this.constructor.currentBlock();
    }

    private static class SsaTranslationVisitor implements Visitor<SsaTranslation, Optional<Node>> {

        @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
        private static final Optional<Node> NOT_AN_EXPRESSION = Optional.empty();

        private final Deque<DebugInfo> debugStack = new ArrayDeque<>();

        private void pushSpan(Tree tree) {
            this.debugStack.push(DebugInfoHelper.getDebugInfo());
            DebugInfoHelper.setDebugInfo(new DebugInfo.SourceInfo(tree.span()));
        }

        private void popSpan() {
            DebugInfoHelper.setDebugInfo(this.debugStack.pop());
        }

        @Override
        public Optional<Node> visit(AssignmentTree assignmentTree, SsaTranslation data) {
            pushSpan(assignmentTree);
            BinaryOperator<Node> desugar = switch (assignmentTree.operator().type()) {
                case ASSIGN_MINUS -> data.constructor::newSub;
                case ASSIGN_PLUS -> data.constructor::newAdd;
                case ASSIGN_MUL -> data.constructor::newMul;
                case ASSIGN_DIV -> (lhs, rhs) -> projResultDivMod(data, data.constructor.newDiv(lhs, rhs));
                case ASSIGN_MOD -> (lhs, rhs) -> projResultDivMod(data, data.constructor.newMod(lhs, rhs));
                case ASSIGN_AND -> data.constructor::newBitwiseAnd;
                case ASSIGN_OR -> data.constructor::newBitwiseOr;
                case ASSIGN_XOR -> data.constructor::newXor;
                case ASSIGN_SHIFT_LEFT -> data.constructor::newShiftLeft;
                case ASSIGN_SHIFT_RIGHT -> data.constructor::newShiftRight;
                case ASSIGN -> null;
                default ->
                    throw new IllegalArgumentException("not an assignment operator " + assignmentTree.operator());
            };

            switch (assignmentTree.lValue()) {
                case LValueIdentTree(var name) -> {
                    Node rhs = assignmentTree.expression().accept(this, data).orElseThrow();
                    if (desugar != null) {
                        rhs = desugar.apply(data.readVariable(name.name(), data.currentBlock()), rhs);
                    }
                    data.writeVariable(name.name(), data.currentBlock(), rhs);
                }
            }
            popSpan();
            return NOT_AN_EXPRESSION;
        }

        @Override
        public Optional<Node> visit(BinaryOperationTree binaryOperationTree, SsaTranslation data) {
            pushSpan(binaryOperationTree);
            Node lhs = binaryOperationTree.lhs().accept(this, data).orElseThrow();
            Node rhs = binaryOperationTree.rhs().accept(this, data).orElseThrow();
            Node res = switch (binaryOperationTree.operatorType()) {
                case MINUS -> data.constructor.newSub(lhs, rhs);
                case PLUS -> data.constructor.newAdd(lhs, rhs);
                case MUL -> data.constructor.newMul(lhs, rhs);
                case DIV -> projResultDivMod(data, data.constructor.newDiv(lhs, rhs));
                case MOD -> projResultDivMod(data, data.constructor.newMod(lhs, rhs));
                case BITWISE_AND -> data.constructor.newBitwiseAnd(lhs, rhs);
                case AND -> data.constructor.newAnd(lhs, rhs);
                case BITWISE_OR -> data.constructor.newBitwiseOr(lhs, rhs);
                case OR -> data.constructor.newOr(lhs, rhs);
                case BITWISE_XOR -> data.constructor.newXor(lhs, rhs);
                case SHIFT_LEFT -> data.constructor.newShiftLeft(lhs, rhs);
                case SHIFT_RIGHT -> data.constructor.newShiftRight(lhs, rhs);
                case LESS -> data.constructor.newLess(lhs, rhs);
                case LESS_EQUAL -> data.constructor.newLessEqual(lhs, rhs);
                case GREATER -> data.constructor.newGreater(rhs, lhs);
                case GREATER_EQUAL -> data.constructor.newGreaterEqual(rhs, lhs);
                case EQUAL -> data.constructor.newEqual(lhs, rhs);
                case NOT_EQUAL -> data.constructor.newNotEqual(lhs, rhs);
                default ->
                    throw new IllegalArgumentException("not a binary expression operator " + binaryOperationTree.operatorType());
            };
            popSpan();
            return Optional.of(res);
        }

        @Override
        public Optional<Node> visit(BlockTree blockTree, SsaTranslation data) {
            pushSpan(blockTree);
            for (StatementTree statement : blockTree.statements()) {
                statement.accept(this, data);
                // skip everything after a return in a block
                if (statement instanceof ReturnTree) {
                    break;
                }
            }
            popSpan();
            return NOT_AN_EXPRESSION;
        }

        @Override
        public Optional<Node> visit(BooleanTree booleanTree, SsaTranslation data) {
            pushSpan(booleanTree);
            Node node = data.constructor.newConstBool(booleanTree.value());
            popSpan();
            return Optional.of(node);
        }

        @Override
        public Optional<Node> visit(BreakTree breakTree, SsaTranslation data) {
            pushSpan(breakTree);
            Node breakNode = data.constructor.newBreak();
            data.constructor.graph().endBlock().addPredecessor(breakNode);
            popSpan();
            return NOT_AN_EXPRESSION;
        }

        @Override
        public Optional<Node> visit(ContinueTree continueTree, SsaTranslation data) {
            pushSpan(continueTree);
            Node continueNode = data.constructor.newContinue();
            data.constructor.graph().endBlock().addPredecessor(continueNode);
            popSpan();
            return NOT_AN_EXPRESSION;
        }

        @Override
        public Optional<Node> visit(DeclarationTree declarationTree, SsaTranslation data) {
            pushSpan(declarationTree);
            ExpressionTree initializer = declarationTree.initializer();
            if (initializer != null) {
                Node rhs = initializer.accept(this, data).orElseThrow();
                data.writeVariable(declarationTree.name().name(), data.currentBlock(), rhs);
            }
            popSpan();
            return NOT_AN_EXPRESSION;
        }

        @Override
        public Optional<Node> visit(ForTree forTree, SsaTranslation data) {
            throw new UnsupportedOperationException("For loops should be desugared before SSA translation");
        }

        @Override
        public Optional<Node> visit(FunctionTree functionTree, SsaTranslation data) {
            pushSpan(functionTree);
            Node start = data.constructor.newStart();
            data.constructor.writeCurrentSideEffect(data.constructor.newSideEffectProj(start));
            functionTree.body().accept(this, data);
            popSpan();
            return NOT_AN_EXPRESSION;
        }

        @Override
        public Optional<Node> visit(IdentExpressionTree identExpressionTree, SsaTranslation data) {
            pushSpan(identExpressionTree);
            Node value = data.readVariable(identExpressionTree.name().name(), data.currentBlock());
            popSpan();
            return Optional.of(value);
        }

        @Override
        public Optional<Node> visit(IfTree ifTree, SsaTranslation data) {
            pushSpan(ifTree);
            Node condition = ifTree.condition().accept(this, data).orElseThrow();

            // Create blocks for then and else branches
            Block thenBlock = data.constructor.newBlock(data.function, "if_then");
            Block joinBlock = data.constructor.newBlock(data.function, "if_join");
            StatementTree elseBranch = ifTree.elseBlock();
            Block elseBlock = elseBranch != null
                ? data.constructor.newBlock(data.function, "if_else")
                : joinBlock;
            Node ifNode = data.constructor.newIf(condition, thenBlock, elseBlock);
            thenBlock.addPredecessor(ifNode);
            elseBlock.addPredecessor(ifNode);

            // then branch
            data.constructor.setCurrentBlock(thenBlock);
            StatementTree thenStatement = ifTree.thenBlock();
            thenStatement.accept(this, data);
            if (!endsWithReturn(thenStatement)) {
                Node jumpToJoin = data.constructor.newJump(joinBlock);
                joinBlock.addPredecessor(jumpToJoin);
            }
            data.constructor.sealBlock(thenBlock);

            // else branch
            if (elseBranch != null) {
                data.constructor.setCurrentBlock(elseBlock);
                elseBranch.accept(this, data);
                if (!endsWithReturn(elseBranch)) {
                    Node jumpToJoin = data.constructor.newJump(joinBlock);
                    joinBlock.addPredecessor(jumpToJoin);
                }
                data.constructor.sealBlock(elseBlock);
            }

            data.constructor.setCurrentBlock(joinBlock);
            data.constructor.sealBlock(joinBlock);
            popSpan();
            return NOT_AN_EXPRESSION;
        }

        @Override
        public Optional<Node> visit(LiteralTree literalTree, SsaTranslation data) {
            pushSpan(literalTree);
            Node node = data.constructor.newConstInt((int) literalTree.parseValue().orElseThrow());
            popSpan();
            return Optional.of(node);
        }

        @Override
        public Optional<Node> visit(LValueIdentTree lValueIdentTree, SsaTranslation data) {
            return NOT_AN_EXPRESSION;
        }

        @Override
        public Optional<Node> visit(NameTree nameTree, SsaTranslation data) {
            return NOT_AN_EXPRESSION;
        }

        @Override
        public Optional<Node> visit(ProgramTree programTree, SsaTranslation data) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Node> visit(ReturnTree returnTree, SsaTranslation data) {
            pushSpan(returnTree);
            Node node = returnTree.expression().accept(this, data).orElseThrow();
            Node ret = data.constructor.newReturn(node);
            data.constructor.graph().endBlock().addPredecessor(ret);
            popSpan();
            return NOT_AN_EXPRESSION;
        }

        @Override
        public Optional<Node> visit(TernaryOperationTree ternaryOperationTree, SsaTranslation data) {
            pushSpan(ternaryOperationTree);

            // Evaluate condition
            Node condition = ternaryOperationTree.condition().accept(this, data).orElseThrow();

            // Create blocks
            Block trueBlock = data.constructor.newBlock(data.function, "if_true");
            Block falseBlock = data.constructor.newBlock(data.function, "if_false");
            Block joinBlock = data.constructor.newBlock(data.function, "if_join");
            Node ternaryNode = data.constructor.newTernary(condition, trueBlock, falseBlock);
            trueBlock.addPredecessor(ternaryNode);
            falseBlock.addPredecessor(ternaryNode);

            // true branch
            data.constructor.setCurrentBlock(trueBlock);
            Node trueResult = ternaryOperationTree.trueBranch().accept(this, data).orElseThrow();
            Node trueExit = data.constructor.newJump(joinBlock);
            data.constructor.sealBlock(trueBlock);

            // false branch
            data.constructor.setCurrentBlock(falseBlock);
            Node falseResult = ternaryOperationTree.falseBranch().accept(this, data).orElseThrow();
            Node falseExit = data.constructor.newJump(joinBlock);
            data.constructor.sealBlock(falseBlock);

            // join block
            joinBlock.addPredecessor(trueExit);
            joinBlock.addPredecessor(falseExit);
            data.constructor.setCurrentBlock(joinBlock);
            data.constructor.sealBlock(joinBlock);

            // Create phi node to merge the values
            Phi phi = new Phi(joinBlock);
            phi.addPredecessor(trueResult);
            phi.addPredecessor(falseResult);
            Node result = data.constructor.tryRemoveTrivialPhi(phi);

            popSpan();
            return Optional.of(result);
        }

        @Override
        public Optional<Node> visit(TypeTree typeTree, SsaTranslation data) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Node> visit(UnaryOperationTree unaryOperationTree, SsaTranslation data) {
            pushSpan(unaryOperationTree);
            Node operand = unaryOperationTree.operand().accept(this, data).orElseThrow();
            Node res = switch (unaryOperationTree.operator().type()) {
                case NOT -> data.constructor.newNot(operand);
                case BITWISE_NOT -> data.constructor.newBitwiseNot(operand);
                case UNARY_MINUS -> data.constructor.newUnaryMinus(operand);
                default ->
                    throw new IllegalArgumentException("not a unary expression operator " + unaryOperationTree.operator().type());
            };
            popSpan();
            return Optional.of(res);
        }

        @Override
        public Optional<Node> visit(WhileTree whileTree, SsaTranslation data) {
            pushSpan(whileTree);

            // Create blocks
            Block conditionBlock = data.constructor.newBlock(data.function, "while_condition");
            Block bodyBlock = data.constructor.newBlock(data.function, "while_body");
            Block afterBlock = data.constructor.newBlock(data.function, "while_after");

            // Add predecessor to condition block from current block
            Node jumpToCond = data.constructor.newJump(conditionBlock);
            conditionBlock.addPredecessor(jumpToCond);

            // condition block
            data.constructor.setCurrentBlock(conditionBlock);
            Node condition = whileTree.condition().accept(this, data).orElseThrow();
            Node ifNode = data.constructor.newIf(condition, bodyBlock, afterBlock);
            bodyBlock.addPredecessor(ifNode);
            afterBlock.addPredecessor(ifNode);

            // while body
            data.constructor.setCurrentBlock(bodyBlock);
            whileTree.body().accept(this, data);
            if (!endsWithReturn(whileTree.body())) {
                Node jumpToCondition = data.constructor.newJump(conditionBlock);
                conditionBlock.addPredecessor(jumpToCondition);
            }
            data.constructor.sealBlock(bodyBlock);
            data.constructor.sealBlock(conditionBlock);

            // after block
            data.constructor.setCurrentBlock(afterBlock);
            data.constructor.sealBlock(afterBlock);

            popSpan();
            return NOT_AN_EXPRESSION;
        }

        private Node projResultDivMod(SsaTranslation data, Node divMod) {
            // make sure we actually have a div or a mod, as optimizations could
            // have changed it to something else already
            if (!(divMod instanceof DivNode || divMod instanceof ModNode)) {
                return divMod;
            }
            Node projSideEffect = data.constructor.newSideEffectProj(divMod);
            data.constructor.writeCurrentSideEffect(projSideEffect);
            return data.constructor.newResultProj(divMod);
        }
    }

    private static boolean endsWithReturn(StatementTree statement) {
        return SemanticAnalysis.containsReturn(statement);
    }

}
