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
                } else if (node instanceof JumpNode || node instanceof TernaryNode || node instanceof ReturnNode) {
                    returnJumpsAndTernaries.add(node);
                } else {
                    others.add(node);
                }
            }

            // Add constants first
            var orderedNodes = new ArrayList<>(constants);

            // Add other nodes, ensuring predecessors come before successors
            for (Node node : others) {
                orderedNodes.add(node);

                // If now anyone has this node as a predecessor, ensure they come after
                for (Node successor : graph.successors(node)) {
                    if (orderedNodes.contains(successor) && orderedNodes.indexOf(successor) < orderedNodes.indexOf(node)) {
                        orderedNodes.remove(successor);
                        orderedNodes.add(orderedNodes.indexOf(node) + 1, successor);
                    }
                }
            }

            // Add jumps and ternaries at the end
            orderedNodes.addAll(returnJumpsAndTernaries);

            order.add(new OrderedBlock(block.name(), orderedNodes));
        }
    }

    private static boolean isRelevant(Node node) {
        return !(node instanceof ProjNode || node instanceof StartNode);
    }
}
