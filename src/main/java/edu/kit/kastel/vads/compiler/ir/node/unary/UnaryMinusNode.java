package edu.kit.kastel.vads.compiler.ir.node.unary;

import edu.kit.kastel.vads.compiler.ir.node.Node;
import edu.kit.kastel.vads.compiler.ir.node.block.Block;

public final class UnaryMinusNode extends UnaryOperationNode {
    public UnaryMinusNode(Block block, Node operand) {
        super(block, operand);
    }
}
