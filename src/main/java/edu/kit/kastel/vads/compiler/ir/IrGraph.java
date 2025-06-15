package edu.kit.kastel.vads.compiler.ir;

import edu.kit.kastel.vads.compiler.ir.node.block.Block;
import edu.kit.kastel.vads.compiler.ir.node.Node;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.SequencedSet;
import java.util.Set;

public class IrGraph {
    private final Map<Node, SequencedSet<Node>> successors = new IdentityHashMap<>();
    private final Block startBlock;
    private final Block endBlock;
    private final String name;

    public IrGraph(String name) {
        this.name = name;
        this.startBlock = new Block(this, "start", 0);
        this.endBlock = new Block(this, "end", Integer.MAX_VALUE);
    }

    public void registerSuccessor(Node node, Node successor) {
        this.successors.computeIfAbsent(node, _ -> new LinkedHashSet<>()).add(successor);
    }

    public void removeSuccessor(Node node, Node oldSuccessor) {
        this.successors.computeIfAbsent(node, _ -> new LinkedHashSet<>()).remove(oldSuccessor);
    }

    /// {@return the set of nodes that have the given node as one of their inputs}
    public Set<Node> successors(Node node) {
        SequencedSet<Node> successors = this.successors.get(node);
        if (successors == null) {
            return Set.of();
        }
        return Set.copyOf(successors);
    }

    public Block startBlock() {
        return this.startBlock;
    }

    public Block endBlock() {
        return this.endBlock;
    }

    /// {@return the name of this graph}
    public String name() {
        return name;
    }

    public List<Node> getNodesInBlock(Block block) {
        List<Node> nodesInBlock = new ArrayList<>();
        Set<Node> visited = new HashSet<>();
        var queue = new ArrayDeque<Node>();
        queue.add(endBlock());
        while (!queue.isEmpty()) {
            Node node = queue.poll();
            if (node.block().equals(block) && !(node instanceof Block)) {
                nodesInBlock.add(node);
            }

            addAllPredecessors(visited, queue, node);
        }

        return nodesInBlock;
    }

    public List<Block> getBlocks() {
        Set<Block> blocks = new HashSet<>();
        Set<Node> visited = new HashSet<>();
        var queue = new ArrayDeque<Node>();
        queue.add(endBlock());
        while (!queue.isEmpty()) {
            Node node = queue.poll();
            Block block = node.block();
            blocks.add(block);

            addAllPredecessors(visited, queue, node);
        }

        return blocks
            .stream()
            .sorted(Comparator.comparingInt(Block::getIdx))
            .toList();
    }

    private void addAllPredecessors(Set<Node> visited, ArrayDeque<Node> queue, Node node) {
        for (Node predecessor : node.predecessors()) {
            if (visited.add(predecessor)) {
                queue.add(predecessor);
            }
        }

        for (Node predecessor : node.block().predecessors()) {
            if (visited.add(predecessor)) {
                queue.add(predecessor);
            }
        }
    }
}
