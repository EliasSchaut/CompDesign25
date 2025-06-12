package edu.kit.kastel.vads.compiler.ir.node.control;

import edu.kit.kastel.vads.compiler.ir.node.Node;
import edu.kit.kastel.vads.compiler.ir.node.block.Block;

public final class WhileNode extends Node {
    public static final int CONDITION = 0;
    public static final int BODY = 1;
    
    public WhileNode(Block block, Node condition, Node body) {
        super(block, condition, body);
    }
    
    @Override
    protected String info() {
        return "condition: " + predecessor(CONDITION) + ", body: " + predecessor(BODY);
    }
}