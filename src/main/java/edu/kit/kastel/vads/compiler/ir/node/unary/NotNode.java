package edu.kit.kastel.vads.compiler.ir.node.unary;

import edu.kit.kastel.vads.compiler.ir.node.Node;
import edu.kit.kastel.vads.compiler.ir.node.block.Block;

public final class NotNode extends UnaryOperationNode {
    public NotNode(Block block, Node operand) {
        super(block, operand);
    }
}
