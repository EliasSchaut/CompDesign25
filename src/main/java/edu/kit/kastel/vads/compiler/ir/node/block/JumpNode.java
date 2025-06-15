package edu.kit.kastel.vads.compiler.ir.node.block;

import edu.kit.kastel.vads.compiler.ir.node.Node;

public final class JumpNode extends Node {
    private final Block target;

    public JumpNode(Block block, Block target) {
        super(block);
        this.target = target;
    }

    public Block getTarget() {
        return target;
    }
}
