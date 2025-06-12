package edu.kit.kastel.vads.compiler.ir.node.binary;

import edu.kit.kastel.vads.compiler.ir.node.Node;
import edu.kit.kastel.vads.compiler.ir.node.block.Block;

public final class ShiftLeftNode extends BinaryOperationNode {
    public ShiftLeftNode(Block block, Node left, Node right) {
        super(block, left, right);
    }
}
