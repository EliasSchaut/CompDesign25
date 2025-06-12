package edu.kit.kastel.vads.compiler.semantic.analysis.typed;

import edu.kit.kastel.vads.compiler.parser.type.Type;

public record TypedTree<T>(T tree, Type type) {
}
