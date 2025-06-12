package edu.kit.kastel.vads.compiler.ir.node.block;

import edu.kit.kastel.vads.compiler.ir.node.Node;

public final class ContinueNode extends Node {
    public ContinueNode(Block block) {
        super(block);
    }
    
    @Override
    protected String info() {
        return "continue";
    }
}