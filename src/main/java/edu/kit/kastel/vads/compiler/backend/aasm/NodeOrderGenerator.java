package edu.kit.kastel.vads.compiler.backend.aasm;

import edu.kit.kastel.vads.compiler.ir.IrGraph;
import edu.kit.kastel.vads.compiler.ir.node.Node;
import edu.kit.kastel.vads.compiler.ir.node.block.Block;
import java.util.*;

public class NodeOrderGenerator {

    private final Map<String, List<Node>> order = new HashMap<>();

    public NodeOrderGenerator(IrGraph function) {
        generateForGraph(function);
    }

    public Map<String, List<Node>> getOrder() {
        return Collections.unmodifiableMap(order);
    }

    private void generateForGraph(IrGraph graph) {
        Set<Node> visited = new HashSet<>();
        scan(graph.endBlock(), visited);
    }

    private void scan(Node node, Set<Node> visited) {
        Block block = node.block();
        String blockName = block.name();
        for (Node predecessor : node.predecessors()) {
            if (visited.add(predecessor)) {
                scan(predecessor, visited);
                scan(node.block(), visited);
            }
        }

        order.putIfAbsent(blockName, new ArrayList<>());
        List<Node> blockOrder = order.get(blockName);
        if (!blockOrder.contains(node)) {
            blockOrder.add(node);
        }
    }

}
