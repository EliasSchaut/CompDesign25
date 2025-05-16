package edu.kit.kastel.vads.compiler.backend.regalloc;

import edu.kit.kastel.vads.compiler.ir.node.Node;

import java.util.Map;

public interface RegisterSpiller {
    Map<Node, Boolean> spillRegisters(Map<Node, Integer> coloring, int maxSpillSize);
}
