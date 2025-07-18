package edu.kit.kastel.vads.compiler.parser.symbol;

import edu.kit.kastel.vads.compiler.lexer.tokens.Identifier;
import edu.kit.kastel.vads.compiler.lexer.tokens.Keyword;

public sealed interface Name permits IdentName, KeywordName {

    static Name forKeyword(Keyword keyword) {
        return new KeywordName(keyword.type());
    }

    static Name forIdentifier(Identifier identifier) {
        return new IdentName(identifier.value());
    }

    String asString();
}
