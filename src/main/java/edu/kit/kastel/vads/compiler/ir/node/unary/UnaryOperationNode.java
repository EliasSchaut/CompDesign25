package edu.kit.kastel.vads.compiler.ir.node.unary;

import edu.kit.kastel.vads.compiler.ir.node.Node;
import edu.kit.kastel.vads.compiler.ir.node.binary.BinaryOperationNode;
import edu.kit.kastel.vads.compiler.ir.node.block.Block;

public abstract sealed class UnaryOperationNode extends Node
    permits NotNode, BitwiseNotNode, UnaryMinusNode {

    public static final int OPERANT = 0;

    protected UnaryOperationNode(Block block, Node operand) {
        super(block, operand);
    }

    protected static int commutativeHashCode(BinaryOperationNode node) {
        int h = node.block().hashCode();
        h += 31 * (predecessorHash(node, OPERANT));
        return h;
    }
}
