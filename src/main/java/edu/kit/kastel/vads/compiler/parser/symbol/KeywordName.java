package edu.kit.kastel.vads.compiler.parser.symbol;

import edu.kit.kastel.vads.compiler.lexer.tokens.Keyword;

record KeywordName(Keyword.KeywordType type) implements Name {
    @Override
    public String asString() {
        return type().keyword();
    }
}
