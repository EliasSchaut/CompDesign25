package edu.kit.kastel.vads.compiler.ir.node;

public final class UnaryMinusNode extends UnaryOperationNode {
    public UnaryMinusNode(Block block, Node operand) {
        super(block, operand);
    }
}
