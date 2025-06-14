package edu.kit.kastel.vads.compiler.ir.node.block;

import edu.kit.kastel.vads.compiler.ir.node.Node;

public final class JumpNode extends Node {
    public static final int TARGET = 0;

    public JumpNode(Block block, Block target) {
        super(block, target);
    }

}
