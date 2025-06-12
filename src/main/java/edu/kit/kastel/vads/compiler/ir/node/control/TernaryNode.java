package edu.kit.kastel.vads.compiler.ir.node.control;

import edu.kit.kastel.vads.compiler.ir.node.Node;
import edu.kit.kastel.vads.compiler.ir.node.block.Block;

public final class TernaryNode extends Node {
    public static final int CONDITION = 0;
    public static final int THEN_EXPR = 1;
    public static final int ELSE_EXPR = 2;
    
    public TernaryNode(Block block, Node condition, Node thenExpr, Node elseExpr) {
        super(block, condition, thenExpr, elseExpr);
    }
    
    @Override
    protected String info() {
        return "condition: " + predecessor(CONDITION) + 
               ", then: " + predecessor(THEN_EXPR) + 
               ", else: " + predecessor(ELSE_EXPR);
    }
}