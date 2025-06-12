package edu.kit.kastel.vads.compiler.ir.node.binary;

import edu.kit.kastel.vads.compiler.ir.node.block.Block;
import edu.kit.kastel.vads.compiler.ir.node.Node;

public final class LessNode extends BinaryOperationNode {
    public LessNode(Block block, Node left, Node right) {
        super(block, left, right);
    }
}
