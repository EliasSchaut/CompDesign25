package edu.kit.kastel.vads.compiler.ir.node.unary;

import edu.kit.kastel.vads.compiler.ir.node.block.Block;
import edu.kit.kastel.vads.compiler.ir.node.Node;

public final class BitwiseNotNode extends UnaryOperationNode {
    public BitwiseNotNode(Block block, Node operand) {
        super(block, operand);
    }
}
