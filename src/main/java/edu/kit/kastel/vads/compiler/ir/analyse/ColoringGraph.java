package edu.kit.kastel.vads.compiler.ir.analyse;

import edu.kit.kastel.vads.compiler.backend.regalloc.InterferenceGraph;
import edu.kit.kastel.vads.compiler.ir.node.Node;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class ColoringGraph {
    private final InterferenceGraph interferenceGraph;
    private final SimplicialEliminationOrdering simplicialEliminationOrdering;

    public ColoringGraph(InterferenceGraph interferenceGraph, SimplicialEliminationOrdering simplicialEliminationOrdering) {
        this.interferenceGraph = interferenceGraph;
        this.simplicialEliminationOrdering = simplicialEliminationOrdering;
    }

    public Map<Node, Integer> color() {
        var ordering = simplicialEliminationOrdering.getSimplicialEliminationOrdering();

        var colors = new HashMap<Node, Integer>();
        for (Node node : ordering) {
            // Get the colors of the neighbors
            var neighborColors = new HashSet<Integer>();
            for (Node neighbor : interferenceGraph.getNeighbors(node)) {
                if (!colors.containsKey(neighbor)) continue;

                var neighborColor = colors.get(neighbor);
                neighborColors.add(neighborColor);
            }

            // Find the first color not in the neighbor colors
            var color = 0;
            while (neighborColors.contains(color)) {
                color++;
            }

            // Assign the color to the node
            colors.put(node, color);
        }

        return colors;
    }
}
