package edu.kit.kastel.vads.compiler.backend.aasm;

import edu.kit.kastel.vads.compiler.backend.regalloc.Register;
import edu.kit.kastel.vads.compiler.backend.regalloc.RegisterAllocator;
import edu.kit.kastel.vads.compiler.ir.node.Node;
import edu.kit.kastel.vads.compiler.ir.node.block.Block;
import edu.kit.kastel.vads.compiler.ir.node.block.ProjNode;
import edu.kit.kastel.vads.compiler.ir.node.block.ReturnNode;
import edu.kit.kastel.vads.compiler.ir.node.block.StartNode;
import java.util.HashMap;
import java.util.Map;

public class SimpleAasmRegisterAllocator implements RegisterAllocator {
    private final Map<Node, Register> registers = new HashMap<>();
    private int id = 0;

    @Override
    public Map<Node, Register> allocateRegisters(NodeOrderGenerator nodeOrderGenerator) {
        for (String block : nodeOrderGenerator.getOrder().keySet()) {
            for (Node node : nodeOrderGenerator.getOrder().get(block)) {
                if (needsRegister(node)) {
                    this.registers.put(node, new VirtualRegister(id++));
                }
            }
        }

        return registers;
    }

    private static boolean needsRegister(Node node) {
        return !(node instanceof ProjNode || node instanceof StartNode || node instanceof Block || node instanceof ReturnNode);
    }
}
