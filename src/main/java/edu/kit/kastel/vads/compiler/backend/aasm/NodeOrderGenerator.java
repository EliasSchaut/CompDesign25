package edu.kit.kastel.vads.compiler.backend.aasm;

import edu.kit.kastel.vads.compiler.ir.IrGraph;
import edu.kit.kastel.vads.compiler.ir.node.Node;
import edu.kit.kastel.vads.compiler.ir.node.block.Block;
import edu.kit.kastel.vads.compiler.ir.node.block.JumpNode;
import edu.kit.kastel.vads.compiler.ir.node.block.ProjNode;
import edu.kit.kastel.vads.compiler.ir.node.block.ReturnNode;
import edu.kit.kastel.vads.compiler.ir.node.block.StartNode;
import edu.kit.kastel.vads.compiler.ir.node.constant.ConstBoolNode;
import edu.kit.kastel.vads.compiler.ir.node.constant.ConstIntNode;
import edu.kit.kastel.vads.compiler.ir.node.control.TernaryNode;
import java.util.*;

public class NodeOrderGenerator {

    public record OrderedBlock(String blockName, List<Node> nodes) {
    }

    private final List<OrderedBlock> order = new ArrayList<>();

    public NodeOrderGenerator(IrGraph function) {
        generateForGraph(function);
    }

    public List<OrderedBlock> getOrder() {
        return Collections.unmodifiableList(order);
    }

    private void generateForGraph(IrGraph graph) {
        List<Block> blocks = graph.getBlocks();
        for (Block block : blocks) {
            List<Node> allNodes = graph.getNodesInBlock(block)
                .stream()
                .filter(NodeOrderGenerator::isRelevant)
                .toList();

            List<Node> constants = new ArrayList<>();
            List<Node> returnJumpsAndTernaries = new ArrayList<>();
            List<Node> others = new ArrayList<>();

            for (Node node : allNodes) {
                if (node instanceof ConstIntNode || node instanceof ConstBoolNode) {
                    constants.add(node);
                } else if (node instanceof JumpNode || node instanceof TernaryNode ||
                    node instanceof ReturnNode) {
                    returnJumpsAndTernaries.add(node);
                } else {
                    others.add(node);
                }
            }

            // Add constants first
            var orderedNodes = new ArrayList<>(constants);

            // Add other nodes, ensuring predecessors come before successors
            List<Node> nodes = topologicalSort(graph, others);
            orderedNodes.addAll(nodes);

            // Add jumps and ternaries at the end
            orderedNodes.addAll(returnJumpsAndTernaries);

            order.add(new OrderedBlock(block.name(), orderedNodes));
        }
    }

    private List<Node> topologicalSort(IrGraph graph, List<Node> nodes) {
        Map<Node, Integer> inDegree = new HashMap<>();
        Map<Node, List<Node>> adjacencyList = new HashMap<>();

        // Initialize in-degree and adjacency list
        for (Node node : nodes) {
            inDegree.put(node, 0);
            adjacencyList.put(node, new ArrayList<>());
        }

        for (Node node : nodes) {
            for (Node successor : graph.successors(node)) {
                if (successor instanceof ProjNode projNode) {
                    if ((projNode.projectionInfo().equals(
                        ProjNode.SimpleProjectionInfo.SIDE_EFFECT))) {
                        // Side effects shouldn't affect the order
                        continue;
                    }

                    Optional<Node> first = graph.successors(successor).stream().findFirst();
                    if (first.isPresent()) {
                        successor = first.get();
                    } else {
                        // If no successors, we can skip this node
                        continue;
                    }
                }

                if (nodes.contains(successor)) {
                    adjacencyList.get(node).add(successor);
                    inDegree.put(successor, inDegree.get(successor) + 1);
                }
            }
        }

        // Collect nodes with zero in-degree
        Queue<Node> queue = new LinkedList<>();
        for (Map.Entry<Node, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.add(entry.getKey());
            }
        }

        List<Node> sortedNodes = new ArrayList<>();
        while (!queue.isEmpty()) {
            Node current = queue.poll();
            sortedNodes.add(current);

            for (Node successor : adjacencyList.get(current)) {
                inDegree.put(successor, inDegree.get(successor) - 1);
                if (inDegree.get(successor) == 0) {
                    queue.add(successor);
                }
            }
        }

        return sortedNodes;
    }

    private static boolean isRelevant(Node node) {
        return !(node instanceof ProjNode || node instanceof StartNode);
    }
}
