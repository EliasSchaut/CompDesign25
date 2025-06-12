package edu.kit.kastel.vads.compiler.ir.node;

public abstract sealed class UnaryOperationNode extends Node permits NotNode, BitwiseNotNode, UnaryMinusNode {

    public static final int OPERANT = 0;

    public UnaryOperationNode(Block block, Node operand) {
        super(block, operand);
    }

    protected static int commutativeHashCode(BinaryOperationNode node) {
        int h = node.block().hashCode();
        h += 31 * (predecessorHash(node, OPERANT));
        return h;
    }
}
