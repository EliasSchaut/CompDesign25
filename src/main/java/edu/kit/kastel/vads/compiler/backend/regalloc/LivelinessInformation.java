package edu.kit.kastel.vads.compiler.backend.regalloc;

import edu.kit.kastel.vads.compiler.ir.node.Node;

import java.util.Set;

public record LivelinessInformation(Node node, Set<Node> liveIn) {}
