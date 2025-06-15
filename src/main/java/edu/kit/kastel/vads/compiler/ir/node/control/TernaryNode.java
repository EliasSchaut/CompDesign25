package edu.kit.kastel.vads.compiler.ir.node.control;

import edu.kit.kastel.vads.compiler.ir.node.Node;
import edu.kit.kastel.vads.compiler.ir.node.block.Block;

public final class TernaryNode extends Node {
    public static final int CONDITION = 0;
    private final Block thenExpr;
    private final Block elseExpr;

    public TernaryNode(Block block, Node condition, Block thenExpr, Block elseExpr) {
        super(block, condition);
        this.thenExpr = thenExpr;
        this.elseExpr = elseExpr;
    }
    
    @Override
    protected String info() {
        return "condition: " + predecessor(CONDITION) +
               ", then: " + thenExpr +
               ", else: " + elseExpr;
    }

    public Block getThenExpr() {
        return thenExpr;
    }

    public Block getElseExpr() {
        return elseExpr;
    }
}