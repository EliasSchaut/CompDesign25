package edu.kit.kastel.vads.compiler.backend.regalloc;

import edu.kit.kastel.vads.compiler.backend.aasm.NodeOrderGenerator;
import edu.kit.kastel.vads.compiler.ir.node.Node;

import java.util.Map;

public interface RegisterAllocator {
    Map<Node, Register> allocateRegisters(NodeOrderGenerator nodeOrderGenerator);
}
