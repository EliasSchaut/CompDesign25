package edu.kit.kastel.vads.compiler.backend.aasm;

import edu.kit.kastel.vads.compiler.backend.regalloc.Register;
import edu.kit.kastel.vads.compiler.backend.regalloc.RegisterAllocator;
import edu.kit.kastel.vads.compiler.backend.regalloc.RegisterSpiller;
import edu.kit.kastel.vads.compiler.ir.IrGraph;
import edu.kit.kastel.vads.compiler.ir.analyse.ColoringGraph;
import edu.kit.kastel.vads.compiler.ir.node.Block;
import edu.kit.kastel.vads.compiler.ir.node.Node;

import edu.kit.kastel.vads.compiler.ir.node.ProjNode;
import edu.kit.kastel.vads.compiler.ir.node.ReturnNode;
import edu.kit.kastel.vads.compiler.ir.node.StartNode;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AasmRegisterAllocator implements RegisterAllocator {
    private final Map<Node, Register> registers = new HashMap<>();
    private final RegisterSpiller registerSpiller;
    private final ColoringGraph coloringGraph;
    private int id = 0;

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
        AtomicInteger registerId = new AtomicInteger();
        AtomicInteger stackRegisterId = new AtomicInteger(VirtualRegister.MAX_REGISTER_COUNT);
        var spilling = registerSpiller.spillRegisters(coloring, VirtualRegister.MAX_REGISTER_COUNT);
        var colorToRegister =  coloring.values().stream()
            .distinct()
            .collect(Collectors.toMap(Function.identity(), x -> {
                if (Boolean.TRUE.equals(spilling.get(x))) {
                    return new VirtualRegister(stackRegisterId.getAndIncrement());
                } else {
                    return new VirtualRegister(registerId.getAndIncrement());
                }
            }));

        for (Map.Entry<Node, Integer> color : coloring.entrySet()) {
            var node = color.getKey();
            var colorId = color.getValue();
            this.registers.put(node, colorToRegister.get(colorId));
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

    private void scan(Node node, Set<Node> visited) {
        for (Node predecessor : node.predecessors()) {
            if (visited.add(predecessor)) {
                scan(predecessor, visited);
            }
        }

        if (needsRegister(node)) {
            this.registers.put(node, new VirtualRegister(id++));
        }
    }

    private static boolean needsRegister(Node node) {
        return !(node instanceof ProjNode || node instanceof StartNode || node instanceof Block || node instanceof ReturnNode);
    }
}
