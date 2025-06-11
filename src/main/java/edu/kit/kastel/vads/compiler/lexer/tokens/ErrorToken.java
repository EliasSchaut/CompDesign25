package edu.kit.kastel.vads.compiler.lexer.tokens;

import edu.kit.kastel.vads.compiler.Span;

public record ErrorToken(String value, Span span) implements Token {
    @Override
    public String asString() {
        return value();
    }
}
