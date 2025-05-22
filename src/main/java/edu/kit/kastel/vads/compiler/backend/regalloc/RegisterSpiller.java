package edu.kit.kastel.vads.compiler.backend.regalloc;

import edu.kit.kastel.vads.compiler.ir.node.Node;

import java.util.Map;

public interface RegisterSpiller {
    /**
     * Spills registers based on the given coloring and maximum spill size.
     * @param coloring a map of nodes to their assigned colors (registers)
     * @param maxSpillSize the maximum number of registers that can be spilled
     * @return a map of colors to a boolean indicating whether the register should be spilled
     */
    Map<Integer, Boolean> spillRegisters(Map<Node, Integer> coloring, int maxSpillSize);
}
