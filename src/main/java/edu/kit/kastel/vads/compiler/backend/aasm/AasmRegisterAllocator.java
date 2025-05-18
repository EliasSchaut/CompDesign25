package edu.kit.kastel.vads.compiler.backend.aasm;

import edu.kit.kastel.vads.compiler.backend.regalloc.Register;
import edu.kit.kastel.vads.compiler.backend.regalloc.RegisterAllocator;
import edu.kit.kastel.vads.compiler.backend.regalloc.RegisterSpiller;
import edu.kit.kastel.vads.compiler.ir.IrGraph;
import edu.kit.kastel.vads.compiler.ir.analyse.ColoringGraph;
import edu.kit.kastel.vads.compiler.ir.node.Node;

import java.util.HashMap;
import java.util.Map;

public class AasmRegisterAllocator implements RegisterAllocator {
    private final Map<Node, Register> registers = new HashMap<>();
    private final RegisterSpiller registerSpiller;
    private final ColoringGraph coloringGraph;

    public AasmRegisterAllocator(RegisterSpiller registerSpiller, ColoringGraph coloringGraph) {
        this.registerSpiller = registerSpiller;
        this.coloringGraph = coloringGraph;
    }

    @Override
    public Map<Node, Register> allocateRegisters(IrGraph graph) {
        var coloring = coloringGraph.color();
        var maxColor = coloring.values().stream()
                .max(Integer::compareTo)
                .orElseThrow();

        // If our colors fit into the available registers, we can use them directly
        if (maxColor < VirtualRegister.MAX_REGISTER_COUNT) {
            for (Map.Entry<Node, Integer> entry : coloring.entrySet()) {
                var node = entry.getKey();
                var color = entry.getValue();
                this.registers.put(node, new VirtualRegister(color));
            }
            return Map.copyOf(this.registers);
        }

        // Otherwise, we need to spill some registers
        var spilling = registerSpiller.spillRegisters(coloring, VirtualRegister.MAX_REGISTER_COUNT);

        var registerId = 0;
        var stackRegisterId = VirtualRegister.MAX_REGISTER_COUNT;
        for (Map.Entry<Node, Boolean> registerEntry : spilling.entrySet()) {
            var shouldSpill = registerEntry.getValue();
            if (shouldSpill) {
                this.registers.put(registerEntry.getKey(), new VirtualRegister(stackRegisterId++));
            } else {
                this.registers.put(registerEntry.getKey(), new VirtualRegister(registerId++));
            }
        }

        return Map.copyOf(this.registers);
    }

    @Override
    public int getStackSize() {
        int stackRegisters = registers.values().stream()
            .filter(Register::isStackVariable)
            .mapToInt(_ -> 1)
            .sum();

        return stackRegisters * VirtualRegister.REGISTER_BYTE_SIZE;
    }
}
