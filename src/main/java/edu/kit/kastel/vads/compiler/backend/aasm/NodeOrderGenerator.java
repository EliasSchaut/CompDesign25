package edu.kit.kastel.vads.compiler.backend.aasm;

import edu.kit.kastel.vads.compiler.backend.regalloc.Register;
import edu.kit.kastel.vads.compiler.ir.IrGraph;
import edu.kit.kastel.vads.compiler.ir.node.Node;

import java.util.*;

public class NodeOrderGenerator {

    private final List<Node> order = new ArrayList<>();

    public NodeOrderGenerator(List<IrGraph> program) {
        for (IrGraph graph : program) {
            AasmRegisterAllocator allocator = new AasmRegisterAllocator();
            generateForGraph(graph);
        }
    }

    public List<Node> getOrder() {
        return Collections.unmodifiableList(order);
    }

    private void generateForGraph(IrGraph graph) {
        Set<Node> visited = new HashSet<>();
        scan(graph.endBlock(), visited);
    }

    private void scan(Node node, Set<Node> visited) {
        for (Node predecessor : node.predecessors()) {
            if (visited.add(predecessor)) {
                scan(predecessor, visited);
            }
        }
        order.add(node);
    }

}
