package edu.kit.kastel.vads.compiler.ir.node.control;

import edu.kit.kastel.vads.compiler.ir.node.Node;
import edu.kit.kastel.vads.compiler.ir.node.block.Block;

public final class IfNode extends Node {
    public static final int CONDITION = 0;
    public static final int THEN = 1;
    public static final int ELSE = 2;

    public IfNode(Block block, Node condition, Node thenBlock, Node elseBlock) {
        super(block, condition, thenBlock, elseBlock);
    }
    
    @Override
    protected String info() {
        return "condition: " + predecessor(CONDITION) + ", then: " + predecessor(THEN) +
               (predecessorCount() > ELSE ? ", else: " + predecessor(ELSE) : "");
    }

    private int predecessorCount() {
        return predecessors().size();
    }
}