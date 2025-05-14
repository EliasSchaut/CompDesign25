package edu.kit.kastel.vads.compiler.ir.analyse;

import edu.kit.kastel.vads.compiler.ir.node.Node;

import java.util.List;

public interface SimplicialEliminationOrdering {
    List<Node> getSimplicialEliminationOrdering();
}
