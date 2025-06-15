package edu.kit.kastel.vads.compiler.ir.node.binary;

import edu.kit.kastel.vads.compiler.ir.node.block.Block;
import edu.kit.kastel.vads.compiler.ir.node.Node;

public final class GreaterNode extends BinaryOperationNode {
    public GreaterNode(Block block, Node left, Node right) {
        super(block, right, left);
    }
}

