package edu.kit.kastel.vads.compiler.backend.aasm;

import edu.kit.kastel.vads.compiler.backend.regalloc.RegisterSpiller;
import edu.kit.kastel.vads.compiler.ir.node.*;

import java.util.*;

public class LeastUsedRegisterSpiller implements RegisterSpiller {
    public Map<Integer, Boolean> spillRegisters(Map<Node, Integer> coloring, int maxSpillSize) {
        // 1 color = 1 register
        var colorUsageCount = new HashMap<Integer, Integer>();
        for (Map.Entry<Node, Integer> colorEntry : coloring.entrySet()) {
            colorUsageCount.merge(colorEntry.getValue(), 1, Integer::sum);
        }

        List<Map.Entry<Integer, Integer>> sortedColorUsages = colorUsageCount.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue())
                .toList();

        var shouldSpill = new HashMap<Integer, Boolean>();
        for (int i = 0; i < sortedColorUsages.size(); i++) {
            shouldSpill.put(sortedColorUsages.get(i).getKey(), i < maxSpillSize);
        }

        return shouldSpill;
    }
}
