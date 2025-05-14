package edu.kit.kastel.vads.compiler.ir.analyse;

import edu.kit.kastel.vads.compiler.backend.regalloc.InterferenceGraph;
import edu.kit.kastel.vads.compiler.ir.node.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MaximumCardinalitySearch implements SimplicialEliminationOrdering {
    private final InterferenceGraph interferenceGraph;

    public MaximumCardinalitySearch(InterferenceGraph interferenceGraph) {
        this.interferenceGraph = interferenceGraph;
    }

    public List<Node> getSimplicialEliminationOrdering() {
        Map<Node, Integer> variables = interferenceGraph.getVariables()
                .stream()
                .collect(Collectors.toMap(node -> node, _ -> 0));

        var ordering = new ArrayList<Node>();
        while (!variables.isEmpty()) {
            // Find the variable with the minimum weight
            var minimumVariable = variables.entrySet().stream()
                    .min(Map.Entry.comparingByValue())
                    .orElseThrow()
                    .getKey();

            // Remove the variable from the graph and update the weights of its neighbors
            variables.remove(minimumVariable);
            for (Node neighbor : interferenceGraph.getNeighbors(minimumVariable)) {
                if (!variables.containsKey(neighbor)) continue;

                variables.merge(neighbor, 1, Integer::sum);
            }

            // Add the variable to the ordering
            ordering.add(minimumVariable);
        }

        return ordering;
    }
}
