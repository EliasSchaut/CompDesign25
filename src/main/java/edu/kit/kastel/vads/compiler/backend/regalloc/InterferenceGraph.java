package edu.kit.kastel.vads.compiler.backend.regalloc;

import edu.kit.kastel.vads.compiler.ir.analyse.LivelinessInformation;
import edu.kit.kastel.vads.compiler.ir.node.Node;

import java.util.*;

public class InterferenceGraph {
    private final Map<Node, Set<Node>> neighbors = new HashMap<>();

    public InterferenceGraph(List<LivelinessInformation> livelinessInformation) {
        for (var info : livelinessInformation) {
            // Create edge based on cartesian product of liveIn nodes
            for (Node node : info.liveIn()) {
                for (Node otherNode : info.liveIn()) {
                    if (node.equals(otherNode)) continue;

                    neighbors.computeIfAbsent(node, _ -> new HashSet<>()).add(otherNode);
                    neighbors.computeIfAbsent(otherNode, _ -> new HashSet<>()).add(node);
                }
            }
        }

        // Add any missing nodes to the graph
        for (var info : livelinessInformation) {
            neighbors.putIfAbsent(info.node(), Set.of());
        }
    }

    public List<Node> getVariables() {
        return new ArrayList<>(neighbors.keySet());
    }

    public List<Node> getNeighbors(Node variable) {
        return new ArrayList<>(neighbors.getOrDefault(variable, Set.of()));
    }
}
