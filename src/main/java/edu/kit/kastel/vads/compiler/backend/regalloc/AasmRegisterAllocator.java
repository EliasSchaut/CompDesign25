package edu.kit.kastel.vads.compiler.backend.regalloc;

import edu.kit.kastel.vads.compiler.backend.aasm.NodeOrderGenerator;
import edu.kit.kastel.vads.compiler.ir.node.Node;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AasmRegisterAllocator implements RegisterAllocator {
    @Override
    public Map<Node, Register> allocateRegisters(NodeOrderGenerator nodeOrderGenerator) {
        var livelinessAnalysis = new LivelinessAnalysis(nodeOrderGenerator);
        var livelinessInfo = livelinessAnalysis.analyze();
        var interferenceGraph = new InterferenceGraph(livelinessInfo);
        var simplicialEliminationOrdering = new MaximumCardinalitySearch(interferenceGraph);
        var coloringGraph = new ColoringGraph(interferenceGraph, simplicialEliminationOrdering);
        var registerSpiller = new LeastUsedRegisterSpiller();

        var registers = new HashMap<Node, Register>();
        var coloring = coloringGraph.color();
        var maxColor = coloring.values().stream()
            .max(Integer::compareTo)
            .orElseThrow();

        // If our colors fit into the available registers, we can use them directly
        if (maxColor < VirtualRegister.MAX_REGISTER_COUNT) {
            for (Map.Entry<Node, Integer> entry : coloring.entrySet()) {
                var node = entry.getKey();
                var color = entry.getValue();
                registers.put(node, new VirtualRegister(color));
            }
            return Map.copyOf(registers);
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
            registers.put(node, colorToRegister.get(colorId));
        }

        return Map.copyOf(registers);
    }
}
