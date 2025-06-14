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
            pushSpan(forTree);

            // Process initialization if it exists
            if (forTree.init() != null) {
                forTree.init().accept(this, data);
            }

            // Create blocks for condition, body, update, and after for
            Block conditionBlock = new Block(data.constructor.graph());
            Block bodyBlock = new Block(data.constructor.graph());
            Block updateBlock = new Block(data.constructor.graph());
            Block afterBlock = new Block(data.constructor.graph());

            // Add predecessor to condition block from current block
            conditionBlock.addPredecessor(data.constructor.readCurrentSideEffect());

            // Set current block to condition block
            data.constructor.setCurrentBlock(conditionBlock);

            // Evaluate condition
            Node condition = forTree.condition().accept(this, data).orElseThrow();

            // Process update if it exists
            Node update = null;
            if (forTree.update() != null) {
                data.constructor.setCurrentBlock(updateBlock);
                forTree.update().accept(this, data);
                update = data.constructor.readCurrentSideEffect();

                // Add loop back from update to condition
                conditionBlock.addPredecessor(update);
            }

            // Create for node
            data.constructor.setCurrentBlock(conditionBlock);
            Node forNode = data.constructor.newFor(
                data.constructor.readCurrentSideEffect(), 
                condition, 
                update != null ? update : data.constructor.readCurrentSideEffect(), 
                bodyBlock
            );

            // Add predecessors to body and after blocks
            bodyBlock.addPredecessor(forNode);
            afterBlock.addPredecessor(forNode);

            // Process body
            data.constructor.setCurrentBlock(bodyBlock);
            forTree.body().accept(this, data);
            Node bodyEnd = data.constructor.readCurrentSideEffect();

            // Add loop back from body to update or condition
            if (update != null) {
                updateBlock.addPredecessor(bodyEnd);
            } else {
                conditionBlock.addPredecessor(bodyEnd);
            }

            // Set current block to after block
            data.constructor.setCurrentBlock(afterBlock);

            popSpan();
            return NOT_AN_EXPRESSION;
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
            Block thenBlock = new Block(data.constructor.graph());
            Block elseBlock = new Block(data.constructor.graph());
            Block joinBlock = new Block(data.constructor.graph());
            Node ifNode = data.constructor.newIf(condition, thenBlock, elseBlock);

            // then branch
            thenBlock.addPredecessor(ifNode);
            data.constructor.setCurrentBlock(thenBlock);
            ifTree.thenBlock().accept(this, data);
            Node thenEnd = data.constructor.readCurrentSideEffect();
            joinBlock.addPredecessor(thenEnd);
            data.constructor.sealBlock(thenBlock);

            // else branch if it exists
            if (ifTree.elseBlock() != null) {
                elseBlock.addPredecessor(ifNode);
                data.constructor.setCurrentBlock(elseBlock);
                ifTree.elseBlock().accept(this, data);
                Node elseEnd = data.constructor.readCurrentSideEffect();
                joinBlock.addPredecessor(elseEnd);
                data.constructor.sealBlock(thenBlock);
            }

            data.constructor.setCurrentBlock(joinBlock);
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

            // Create blocks for true branch, false branch, and join
            Block trueBlock = new Block(data.constructor.graph());
            Block falseBlock = new Block(data.constructor.graph());
            Block joinBlock = new Block(data.constructor.graph());

            // Create ternary node
            Node ternaryNode = data.constructor.newTernary(condition, trueBlock, falseBlock);

            // Add predecessors to true and false blocks
            trueBlock.addPredecessor(ternaryNode);
            falseBlock.addPredecessor(ternaryNode);

            // Process true branch
            data.constructor.setCurrentBlock(trueBlock);
            Node trueResult = ternaryOperationTree.trueBranch().accept(this, data).orElseThrow();
            //joinBlock.addPredecessor(data.constructor.readCurrentSideEffect());
            data.constructor.sealBlock(trueBlock);

            // Process false branch
            data.constructor.setCurrentBlock(falseBlock);
            Node falseResult = ternaryOperationTree.falseBranch().accept(this, data).orElseThrow();
            //joinBlock.addPredecessor(data.constructor.readCurrentSideEffect());
            data.constructor.sealBlock(falseBlock);

            // Set current block to join block
            data.constructor.setCurrentBlock(joinBlock);

            // Create phi node for the result
            //Phi resultPhi = data.constructor.newPhi();
            //resultPhi.appendOperand(trueResult);
            //resultPhi.appendOperand(falseResult);

            popSpan();
            return Optional.of(ternaryNode);
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

            // Create blocks for condition, body, and after while
            Block conditionBlock = new Block(data.constructor.graph());
            Block bodyBlock = new Block(data.constructor.graph());
            Block afterBlock = new Block(data.constructor.graph());

            // Add predecessor to condition block from current block
            Block currentBlock = data.constructor.currentBlock();
            conditionBlock.addPredecessor(data.constructor.readCurrentSideEffect());

            // Set current block to condition block
            data.constructor.setCurrentBlock(conditionBlock);

            // Evaluate condition
            Node condition = whileTree.condition().accept(this, data).orElseThrow();

            // Create while node
            Node whileNode = data.constructor.newWhile(condition, bodyBlock);

            // Add predecessors to body and after blocks
            bodyBlock.addPredecessor(whileNode);
            afterBlock.addPredecessor(whileNode);

            // Process body
            data.constructor.setCurrentBlock(bodyBlock);
            whileTree.body().accept(this, data);
            Node bodyEnd = data.constructor.readCurrentSideEffect();

            // Add loop back from body to condition
            conditionBlock.addPredecessor(bodyEnd);

            // Set current block to after block
            data.constructor.setCurrentBlock(afterBlock);

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


}
