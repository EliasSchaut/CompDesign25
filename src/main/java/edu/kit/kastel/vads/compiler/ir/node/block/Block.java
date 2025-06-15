package edu.kit.kastel.vads.compiler.ir.node.block;

import edu.kit.kastel.vads.compiler.ir.IrGraph;
import edu.kit.kastel.vads.compiler.ir.node.Node;

public final class Block extends Node {

    private final String name;
    private final int idx;

    public Block(IrGraph graph, String name, int idx) {
        super(graph);
        this.name = name;
        this.idx = idx;
    }

    public String name() {
        return name;
    }

    public int getIdx() {
        return idx;
    }
}
