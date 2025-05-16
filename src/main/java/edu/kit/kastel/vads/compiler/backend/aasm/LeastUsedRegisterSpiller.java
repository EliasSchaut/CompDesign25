package edu.kit.kastel.vads.compiler.backend.aasm;

import edu.kit.kastel.vads.compiler.backend.regalloc.RegisterSpiller;
import edu.kit.kastel.vads.compiler.ir.node.*;

import java.util.*;

public class LeastUsedRegisterSpiller implements RegisterSpiller {
    public Map<Node, Boolean> spillRegisters(Map<Node, Integer> coloring, int maxSpillSize) {
        // 1 color = 1 register
        var colorUsageCount = new HashMap<Integer, Integer>();
        for (Map.Entry<Node, Integer> colorEntry : coloring.entrySet()) {
            colorUsageCount.merge(colorEntry.getValue(), 1, Integer::sum);
        }

        List<Map.Entry<Integer, Integer>> sortedColorUsages = colorUsageCount.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue())
                .toList();

        var shouldSpill = new HashMap<Node, Boolean>();
        for (int i = 0; i < sortedColorUsages.size(); i++) {
            Map.Entry<Integer, Integer> entry = sortedColorUsages.get(i);
            int color = entry.getKey();
            var nodesWithColor = coloring.entrySet()
                    .stream()
                    .filter(e -> e.getValue() == color)
                    .map(Map.Entry::getKey)
                    .toList();

            var shouldSpillNode = i > maxSpillSize;
            for (Node node : nodesWithColor) {
                shouldSpill.put(node, shouldSpillNode);
            }
        }

        return shouldSpill;
    }

    private void scan(Node node, Set<Node> visited, Map<Node, Integer> usageCount) {
        for (Node predecessor : node.predecessors()) {
            if (visited.add(predecessor)) {
                scan(predecessor, visited, usageCount);
            }
        }

        if (needsRegister(node)) {
            usageCount.merge(node, 1, Integer::sum);
        }
    }

    private static boolean needsRegister(Node node) {
        return !(node instanceof ProjNode || node instanceof StartNode || node instanceof Block || node instanceof ReturnNode);
    }
}
