package edu.kit.kastel.vads.compiler.ir.node.control;

import edu.kit.kastel.vads.compiler.ir.node.Node;
import edu.kit.kastel.vads.compiler.ir.node.block.Block;

public final class IfNode extends Node {
    public static final int CONDITION = 0;
    private final Block thenBlock;
    private final Block elseBlock;

    public IfNode(Block block, Node condition, Block thenBlock, Block elseBlock) {
        super(block, condition);
        this.thenBlock = thenBlock;
        this.elseBlock = elseBlock;
    }

    @Override
    protected String info() {
        return "condition: " + predecessor(CONDITION);
    }

    public Block getThenBlock() {
        return thenBlock;
    }

    public Block getElseBlock() {
        return elseBlock;
    }
}