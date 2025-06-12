
package edu.kit.kastel.vads.compiler.ir.node.block;

import edu.kit.kastel.vads.compiler.ir.node.Node;

public final class BreakNode extends Node {
    public BreakNode(Block block) {
        super(block);
    }
    
    @Override
    protected String info() {
        return "break";
    }
}