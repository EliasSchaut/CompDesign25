package edu.kit.kastel.vads.compiler.backend.regalloc;

import static edu.kit.kastel.vads.compiler.ir.util.NodeSupport.predecessorSkipProj;

import edu.kit.kastel.vads.compiler.backend.aasm.NodeOrderGenerator;
import edu.kit.kastel.vads.compiler.ir.node.Node;
import edu.kit.kastel.vads.compiler.ir.node.binary.BinaryOperationNode;
import edu.kit.kastel.vads.compiler.ir.node.block.ReturnNode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LivelinessAnalysis {

    private final List<Node> orderedNodes;

    public LivelinessAnalysis(NodeOrderGenerator nodeOrderGenerator) {
        // TODO make this multi block ready
        this.orderedNodes = nodeOrderGenerator.getOrder().stream().flatMap(x -> x.nodes().stream()).toList();
    }

    public List<LivelinessInformation> analyze() {
        List<LivelinessInformation> livenessInfos = new ArrayList<>();
        Set<Node> liveLastLine = Set.of();
        // TODO: For jumps, we need to iterate multiple times to saturate the liveness information (L5 - L8)
        for (int i = orderedNodes.size() - 1; i >= 0; i--) {
            var node = orderedNodes.get(i);
            Set<Node> liveIn = new HashSet<>();
            switch (node) {
                // L1
                case BinaryOperationNode binary -> {
                    liveIn.add(predecessorSkipProj(binary, BinaryOperationNode.LEFT));
                    liveIn.add(predecessorSkipProj(binary, BinaryOperationNode.RIGHT));
                }
                // L3
                case ReturnNode r -> liveIn.add(predecessorSkipProj(r, ReturnNode.RESULT));
                default -> { /* do nothing */ }
            }

            // L2 & L4
            for (Node nodeLiveLast : liveLastLine) {
                if (node != nodeLiveLast) {
                    liveIn.add(nodeLiveLast);
                }
            }

            liveLastLine = liveIn;
            var livenessInfo = new LivelinessInformation(node, liveIn);
            livenessInfos.add(livenessInfo);
        }
        return livenessInfos.reversed();
    }
}
