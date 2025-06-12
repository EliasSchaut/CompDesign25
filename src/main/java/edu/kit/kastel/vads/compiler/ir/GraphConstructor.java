package edu.kit.kastel.vads.compiler.ir;

import edu.kit.kastel.vads.compiler.ir.node.*;
import edu.kit.kastel.vads.compiler.ir.node.binary.AddNode;
import edu.kit.kastel.vads.compiler.ir.node.binary.AndNode;
import edu.kit.kastel.vads.compiler.ir.node.binary.BitwiseAndNode;
import edu.kit.kastel.vads.compiler.ir.node.binary.BitwiseOrNode;
import edu.kit.kastel.vads.compiler.ir.node.binary.DivNode;
import edu.kit.kastel.vads.compiler.ir.node.binary.GreaterEqualNode;
import edu.kit.kastel.vads.compiler.ir.node.binary.GreaterNode;
import edu.kit.kastel.vads.compiler.ir.node.binary.LessEqualNode;
import edu.kit.kastel.vads.compiler.ir.node.binary.LessNode;
import edu.kit.kastel.vads.compiler.ir.node.binary.ModNode;
import edu.kit.kastel.vads.compiler.ir.node.binary.MulNode;
import edu.kit.kastel.vads.compiler.ir.node.binary.OrNode;
import edu.kit.kastel.vads.compiler.ir.node.Phi;
import edu.kit.kastel.vads.compiler.ir.node.binary.ShiftLeftNode;
import edu.kit.kastel.vads.compiler.ir.node.binary.ShiftRightNode;
import edu.kit.kastel.vads.compiler.ir.node.binary.SubNode;
import edu.kit.kastel.vads.compiler.ir.node.binary.XorNode;
import edu.kit.kastel.vads.compiler.ir.node.block.Block;
import edu.kit.kastel.vads.compiler.ir.node.block.ProjNode;
import edu.kit.kastel.vads.compiler.ir.node.block.ReturnNode;
import edu.kit.kastel.vads.compiler.ir.node.block.StartNode;
import edu.kit.kastel.vads.compiler.ir.node.constant.ConstBoolNode;
import edu.kit.kastel.vads.compiler.ir.node.constant.ConstIntNode;
import edu.kit.kastel.vads.compiler.ir.node.unary.BitwiseNotNode;
import edu.kit.kastel.vads.compiler.ir.node.unary.NotNode;
import edu.kit.kastel.vads.compiler.ir.node.unary.UnaryMinusNode;
import edu.kit.kastel.vads.compiler.ir.optimize.Optimizer;
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
    private Block currentBlock;

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
    public Node newStart() {
        assert currentBlock() == this.graph.startBlock() : "start must be in start block";
        return new StartNode(currentBlock());
    }

    public Node newReturn(Node result) {
        return new ReturnNode(currentBlock(), readCurrentSideEffect(), result);
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
    //public Node newIf(Node condition, Node thenBranch, Node elseBranch) {
    //    return this.optimizer.transform(new IfNode(currentBlock(), condition, thenBranch, elseBranch));
    //}
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
            val = newPhi();
            this.incompletePhis.computeIfAbsent(block, _ -> new HashMap<>()).put(variable, (Phi) val);
        } else if (block.predecessors().size() == 1) {
            val = readVariable(variable, block.predecessors().getFirst().block());
        } else {
            val = newPhi();
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
        // TODO: the paper shows how to remove trivial phis.
        // as this is not a problem in Lab 1 and it is just
        // a simplification, we recommend to implement this
        // part yourself.
        return phi;
    }

    void sealBlock(Block block) {
        for (Map.Entry<Name, Phi> entry : this.incompletePhis.getOrDefault(block, Map.of()).entrySet()) {
            addPhiOperands(entry.getKey(), entry.getValue());
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
            val = newPhi();
            Phi old = this.incompleteSideEffectPhis.put(block, (Phi) val);
            assert old == null : "double readSideEffectRecursive for " + block;
        } else if (block.predecessors().size() == 1) {
            val = readSideEffect(block.predecessors().getFirst().block());
        } else {
            val = newPhi();
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

}
