package edu.kit.kastel.vads.compiler.backend.aasm;

import edu.kit.kastel.vads.compiler.ir.IrGraph;
import edu.kit.kastel.vads.compiler.ir.node.Node;
import edu.kit.kastel.vads.compiler.ir.node.block.Block;
import edu.kit.kastel.vads.compiler.ir.node.block.JumpNode;
import edu.kit.kastel.vads.compiler.ir.node.block.ProjNode;
import edu.kit.kastel.vads.compiler.ir.node.block.StartNode;
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
        List<Block> blocks = graph.getBlocks();
        for (Block block : blocks) {
            var nodes = new ArrayList<>(graph
                .getNodesInBlock(block)
                .stream()
                .filter(NodeOrderGenerator::isRelevant)
                .sorted((o1, o2) -> {
                    if (o1 instanceof JumpNode) {
                        return 1; // Jump nodes should come after all other nodes
                    }
                    if (o2 instanceof JumpNode) {
                        return -1; // Jump nodes should come after all other nodes
                    }

                    if (o1.isRecursivePredecessor(o2)) {
                        return 1;
                    } else if (o2.isRecursivePredecessor(o1)) {
                        return -1;
                    } else {
                        return -1;
                    }
                })
                .toList());

            order.put(block.name(), nodes);
        }

    }

    private static boolean isRelevant(Node node) {
        return !(node instanceof ProjNode || node instanceof StartNode);
    }
}
