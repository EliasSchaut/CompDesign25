package edu.kit.kastel.vads.compiler.lexer.tokens;

import edu.kit.kastel.vads.compiler.Span;

public record Operator(OperatorType type, Span span) implements Token {

    @Override
    public boolean isOperator(OperatorType operatorType) {
        return type() == operatorType;
    }

    @Override
    public String asString() {
        return type().toString();
    }

    // Ordered by precedence, from highest to lowest
    public enum OperatorType {
        NOT("!"),
        BITWISE_NOT("~"),
        UNARY_MINUS("-"),

        MUL("*"),
        DIV("/"),
        MOD("%"),

        PLUS("+"),
        MINUS("-"),

        SHIFT_LEFT("<<"),
        SHIFT_RIGHT(">>"),

        LESS("<"),
        LESS_EQUAL("<="),
        GREATER(">"),
        GREATER_EQUAL(">="),

        EQUAL("=="),
        NOT_EQUAL("!="),

        BITWISE_AND("&"),
        BITWISE_OR("|"),
        BITWISE_XOR("^"),
        AND("&&"),
        OR("||"),

        TERNARY_CONDITION("?"),
        TERNARY_COLON(":"),

        ASSIGN("="),
        ASSIGN_MINUS("-="),
        ASSIGN_PLUS("+="),
        ASSIGN_MUL("*="),
        ASSIGN_DIV("/="),
        ASSIGN_MOD("%="),
        ASSIGN_AND("&="),
        ASSIGN_XOR("^="),
        ASSIGN_OR("|="),
        ASSIGN_SHIFT_LEFT("<<="),
        ASSIGN_SHIFT_RIGHT(">>="),
        ;

        private final String value;

        OperatorType(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }
    }
}
