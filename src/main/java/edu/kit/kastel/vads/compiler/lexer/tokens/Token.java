package edu.kit.kastel.vads.compiler.lexer.tokens;

import edu.kit.kastel.vads.compiler.Span;

public sealed interface Token permits ErrorToken, Identifier, Keyword, NumberLiteral, Operator,
    Separator {

    Span span();

    default boolean isKeyword(Keyword.KeywordType keywordType) {
        return false;
    }

    default boolean isOperator(Operator.OperatorType operatorType) {
        return false;
    }

    default boolean isSeparator(Separator.SeparatorType separatorType) {
        return false;
    }

    String asString();
}
