package edu.kit.kastel.vads.compiler.ir.node.block;

import edu.kit.kastel.vads.compiler.ir.IrGraph;
import edu.kit.kastel.vads.compiler.ir.node.Node;

public final class Block extends Node {

    private final String name;

    public Block(IrGraph graph, String name) {
        super(graph);
        this.name = name;
    }

    public String name() {
        return name;
    }

}
