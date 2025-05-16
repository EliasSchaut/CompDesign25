package edu.kit.kastel.vads.compiler.ir.analyse;

import edu.kit.kastel.vads.compiler.ir.node.Node;

import java.util.Set;

public record LivelinessInformation(Node node, Set<Node> liveIn) {}
