package edu.kit.kastel.vads.compiler.ir.node.control;

import edu.kit.kastel.vads.compiler.ir.node.Node;
import edu.kit.kastel.vads.compiler.ir.node.block.Block;

public final class ForNode extends Node {
    public static final int INIT = 0;
    public static final int CONDITION = 1;
    public static final int UPDATE = 2;
    public static final int BODY = 3;
    
    public ForNode(Block block, Node init, Node condition, Node update, Node body) {
        super(block, init, condition, update, body);
    }
    
    @Override
    protected String info() {
        return "init: " + predecessor(INIT) + 
               ", condition: " + predecessor(CONDITION) + 
               ", update: " + predecessor(UPDATE) + 
               ", body: " + predecessor(BODY);
    }
}