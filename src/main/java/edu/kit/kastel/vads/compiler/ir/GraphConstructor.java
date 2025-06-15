package edu.kit.kastel.vads.compiler.ir;

import edu.kit.kastel.vads.compiler.ir.node.*;
import edu.kit.kastel.vads.compiler.ir.node.binary.*;
import edu.kit.kastel.vads.compiler.ir.node.block.*;
import edu.kit.kastel.vads.compiler.ir.node.control.IfNode;
import edu.kit.kastel.vads.compiler.ir.node.Phi;
import edu.kit.kastel.vads.compiler.ir.node.control.TernaryNode;
import edu.kit.kastel.vads.compiler.ir.node.constant.ConstBoolNode;
import edu.kit.kastel.vads.compiler.ir.node.constant.ConstIntNode;
import edu.kit.kastel.vads.compiler.ir.node.unary.BitwiseNotNode;
import edu.kit.kastel.vads.compiler.ir.node.unary.NotNode;
import edu.kit.kastel.vads.compiler.ir.node.unary.UnaryMinusNode;
import edu.kit.kastel.vads.compiler.ir.optimize.Optimizer;
import edu.kit.kastel.vads.compiler.parser.ast.FunctionTree;
import edu.kit.kastel.vads.compiler.parser.symbol.Name;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class GraphConstructor {

    private final Optimizer optimizer;
    private final IrGraph graph;
    private final Map<Name, Map<Block, Node>> currentDef = new HashMap<>();
    private final Map<Block, Map<Name, Phi>> incompletePhis = new HashMap<>();
    private final Map<Block, Node> currentSideEffect = new HashMap<>();
    private final Map<Block, Phi> incompleteSideEffectPhis = new HashMap<>();
    private final Set<Block> sealedBlocks = new HashSet<>();
    private final HashSet<String> blockNames = new HashSet<>();
    private Block currentBlock;
    private int nextBlockId = 1;

    public GraphConstructor(Optimizer optimizer, String name) {
        this.optimizer = optimizer;
        this.graph = new IrGraph(name);
        this.currentBlock = this.graph.startBlock();
        // the start block never gets any more predecessors
        sealBlock(this.currentBlock);
    }


    // ----------
    // blocks operations
    // ----------
    public Block newBlock(FunctionTree function, String suffix) {
        return new Block(this.graph, getBlockName(function, suffix), nextBlockId++);
    }

    public Node newStart() {
        assert currentBlock() == this.graph.startBlock() : "start must be in start block";
        return new StartNode(currentBlock());
    }

    public Node newReturn(Node result) {
        return new ReturnNode(currentBlock(), readCurrentSideEffect(), result);
    }

    public Node newJump(Block target) {
        return this.optimizer.transform(new JumpNode(currentBlock(), target));
    }
    // ----------

    // ----------
    // unary operations
    // ----------
    public Node newNot(Node operand) {
        return this.optimizer.transform(new NotNode(currentBlock(), operand));
    }

    public Node newBitwiseNot(Node operand) {
        return this.optimizer.transform(new BitwiseNotNode(currentBlock(), operand));
    }

    public Node newUnaryMinus(Node operand) {
        return this.optimizer.transform(new UnaryMinusNode(currentBlock(), operand));
    }
    // ----------

    // ----------
    // binary operations
    // ----------
    public Node newAdd(Node left, Node right) {
        return this.optimizer.transform(new AddNode(currentBlock(), left, right));
    }

    public Node newSub(Node left, Node right) {
        return this.optimizer.transform(new SubNode(currentBlock(), left, right));
    }

    public Node newMul(Node left, Node right) {
        return this.optimizer.transform(new MulNode(currentBlock(), left, right));
    }

    public Node newDiv(Node left, Node right) {
        return this.optimizer.transform(new DivNode(currentBlock(), left, right, readCurrentSideEffect()));
    }

    public Node newMod(Node left, Node right) {
        return this.optimizer.transform(new ModNode(currentBlock(), left, right, readCurrentSideEffect()));
    }

    public Node newAnd(Node left, Node right) {
        return this.optimizer.transform(new AndNode(currentBlock(), left, right));
    }

    public Node newBitwiseAnd(Node left, Node right) {
        return this.optimizer.transform(new BitwiseAndNode(currentBlock(), left, right));
    }

    public Node newBitwiseOr(Node left, Node right) {
        return this.optimizer.transform(new BitwiseOrNode(currentBlock(), left, right));
    }

    public Node newOr(Node left, Node right) {
        return this.optimizer.transform(new OrNode(currentBlock(), left, right));
    }

    public Node newXor(Node left, Node right) {
        return this.optimizer.transform(new XorNode(currentBlock(), left, right));
    }

    public Node newShiftLeft(Node left, Node right) {
        return this.optimizer.transform(new ShiftLeftNode(currentBlock(), left, right));
    }

    public Node newShiftRight(Node left, Node right) {
        return this.optimizer.transform(new ShiftRightNode(currentBlock(), left, right));
    }

    public Node newLess(Node left, Node right) {
        return this.optimizer.transform(new LessNode(currentBlock(), left, right));
    }

    public Node newLessEqual(Node left, Node right) {
        return this.optimizer.transform(new LessEqualNode(currentBlock(), left, right));
    }

    public Node newGreater(Node left, Node right) {
        return this.optimizer.transform(new GreaterNode(currentBlock(), left, right));
    }

    public Node newGreaterEqual(Node left, Node right) {
        return this.optimizer.transform(new GreaterEqualNode(currentBlock(), left, right));
    }

    public Node newEqual(Node left, Node right) {
        return this.optimizer.transform(new EqualNode(currentBlock(), left, right));
    }

    public Node newNotEqual(Node left, Node right) {
        return this.optimizer.transform(new NotEqualNode(currentBlock(), left, right));
    }
    // ----------


    // ----------
    // constants
    // ----------
    public Node newConstInt(int value) {
        // always move const into start block, this allows better deduplication
        // and resultingly in better value numbering
        return this.optimizer.transform(new ConstIntNode(this.graph.startBlock(), value));
    }

    public Node newConstBool(boolean value) {
        return this.optimizer.transform(new ConstBoolNode(this.graph.startBlock(), value));
    }
    // ----------

    // ----------
    // control flow
    // ----------
    public Node newIf(Node condition, Block thenBlock, Block elseBlock) {
        return this.optimizer.transform(new IfNode(currentBlock(), condition, thenBlock, elseBlock));
    }

    public Node newTernary(Node condition, Block thenBlock, Block elseBlock) {
        return this.optimizer.transform(new TernaryNode(currentBlock(), condition, thenBlock, elseBlock));
    }
    // ----------

    public Node newSideEffectProj(Node node) {
        return new ProjNode(currentBlock(), node, ProjNode.SimpleProjectionInfo.SIDE_EFFECT);
    }

    public Node newResultProj(Node node) {
        return new ProjNode(currentBlock(), node, ProjNode.SimpleProjectionInfo.RESULT);
    }

    public Block currentBlock() {
        return this.currentBlock;
    }

    public void setCurrentBlock(Block block) {
        this.currentBlock = block;
    }

    public Phi newPhi() {
        // don't transform phi directly, it is not ready yet
        return new Phi(currentBlock());
    }


    public IrGraph graph() {
        return this.graph;
    }

    void writeVariable(Name variable, Block block, Node value) {
        this.currentDef.computeIfAbsent(variable, _ -> new HashMap<>()).put(block, value);
    }

    Node readVariable(Name variable, Block block) {
        Node node = this.currentDef.getOrDefault(variable, Map.of()).get(block);
        if (node != null) {
            return node;
        }
        return readVariableRecursive(variable, block);
    }


    private Node readVariableRecursive(Name variable, Block block) {
        Node val;
        if (!this.sealedBlocks.contains(block)) {
            val = new Phi(block);
            this.incompletePhis.computeIfAbsent(block, _ -> new HashMap<>()).put(variable, (Phi) val);
        } else if (block.predecessors().size() == 1) {
            val = readVariable(variable, block.predecessors().getFirst().block());
        } else {
            val = new Phi(block);
            writeVariable(variable, block, val);
            val = addPhiOperands(variable, (Phi) val);
        }
        writeVariable(variable, block, val);
        return val;
    }

    Node addPhiOperands(Name variable, Phi phi) {
        for (Node pred : phi.block().predecessors()) {
            phi.appendOperand(readVariable(variable, pred.block()));
        }
        return tryRemoveTrivialPhi(phi);
    }

    Node tryRemoveTrivialPhi(Phi phi) {
        Node same = null;
        for (Node op : phi.predecessors()) {
            if (op == same || op == phi) {
                continue; // unique value or self-reference
            }
            if (same != null) {
                return phi; // the phi merges at least two values: not trivial
            }
            same = op;
        }

        if (same == null) {
            same = this.newPhi(); // phi is unreachable or in the start block
        }

        // Remember all users except the phi itself
        Set<Node> users = new HashSet<>(phi.graph().successors(phi));
        users.remove(phi);

        // Reroute all uses of phi to same and remove phi
        for (Node use : users) {
            for (int i = 0; i < use.predecessors().size(); i++) {
                if (use.predecessor(i) == phi) {
                    use.setPredecessor(i, same);
                }
            }
        }

        // Try to recursively remove all phi users, which might have become trivial
        for (Node use : users) {
            if (use instanceof Phi) {
                tryRemoveTrivialPhi((Phi) use);
            }
        }
        return same;
    }

    public void removeRemainingPhis() {
        if (!incompletePhis.isEmpty()) {
//            throw new IllegalStateException("There are still incomplete phis in the graph: " + incompletePhis);
        }
    }

    void sealBlock(Block block) {
        for (Map.Entry<Name, Phi> entry : this.incompletePhis.getOrDefault(block, Map.of()).entrySet()) {
            addPhiOperands(entry.getKey(), entry.getValue());
        }
        Phi sideEffectPhi = this.incompleteSideEffectPhis.get(block);
        if (sideEffectPhi != null) {
            addPhiOperands(sideEffectPhi);
        }
        this.sealedBlocks.add(block);
    }

    public void writeCurrentSideEffect(Node node) {
        writeSideEffect(currentBlock(), node);
    }

    private void writeSideEffect(Block block, Node node) {
        this.currentSideEffect.put(block, node);
    }

    public Node readCurrentSideEffect() {
        return readSideEffect(currentBlock());
    }

    private Node readSideEffect(Block block) {
        Node node = this.currentSideEffect.get(block);
        if (node != null) {
            return node;
        }
        return readSideEffectRecursive(block);
    }

    private Node readSideEffectRecursive(Block block) {
        Node val;
        if (!this.sealedBlocks.contains(block)) {
            val = new Phi(block);
            Phi old = this.incompleteSideEffectPhis.put(block, (Phi) val);
            assert old == null : "double readSideEffectRecursive for " + block;
        } else if (block.predecessors().size() == 1 && this.currentSideEffect.get(block.predecessors().getFirst().block()) != null) {
            val = readSideEffect(block.predecessors().getFirst().block());
        } else {
            val = new Phi(block);
            writeSideEffect(block, val);
            val = addPhiOperands((Phi) val);
        }
        writeSideEffect(block, val);
        return val;
    }

    Node addPhiOperands(Phi phi) {
        for (Node pred : phi.block().predecessors()) {
            phi.appendOperand(readSideEffect(pred.block()));
        }
        return tryRemoveTrivialPhi(phi);
    }

    private String getBlockName(FunctionTree function, String suffix) {
        var name = function.name().name().asString() + "_" + suffix;

        if (blockNames.contains(name)) {
            int i = 2;
            while (blockNames.contains(name + "_" + i)) {
                i++;
            }
            name += "_" + i;
        }

        blockNames.add(name);

        return name;
    }

}
