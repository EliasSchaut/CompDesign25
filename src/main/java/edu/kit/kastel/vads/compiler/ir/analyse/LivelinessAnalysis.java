package edu.kit.kastel.vads.compiler.ir.analyse;

import edu.kit.kastel.vads.compiler.backend.aasm.NodeOrderGenerator;
import edu.kit.kastel.vads.compiler.ir.node.*;

import java.util.ArrayList;
import java.util.List;

import static edu.kit.kastel.vads.compiler.ir.util.NodeSupport.predecessorSkipProj;

public class LivelinessAnalysis {

    private final List<Node> orderedNodes;

    public LivelinessAnalysis(NodeOrderGenerator nodeOrderGenerator) {
        this.orderedNodes = nodeOrderGenerator.getOrder();
    }

    public List<LivelinessInformation> analyze() {
        List<LivelinessInformation> livenessInfos = new ArrayList<>();
        // TODO: For jumps, we need to iterate multiple times to saturate the liveness information
        for (int i = orderedNodes.size() - 1; i >= 0 ; i--) {
            var node = orderedNodes.get(i);
            List<Node> live_in;
            switch (node) {
                case BinaryOperationNode binary -> live_in = List.of(
                        predecessorSkipProj(binary, BinaryOperationNode.LEFT),
                        predecessorSkipProj(binary, BinaryOperationNode.RIGHT));
                case ReturnNode r -> live_in = List.of(predecessorSkipProj(r, ReturnNode.RESULT));
                default -> live_in = List.of();
            }
            var livenessInfo = new LivelinessInformation(node, live_in);
            livenessInfos.add(livenessInfo);

        }
        return livenessInfos;
    }
}
