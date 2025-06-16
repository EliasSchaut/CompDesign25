package edu.kit.kastel.vads.compiler.lexer.tokens;

import edu.kit.kastel.vads.compiler.Span;

public record Keyword(KeywordType type, Span span) implements Token {
    @Override
    public boolean isKeyword(KeywordType keywordType) {
        return type() == keywordType;
    }

    @Override
    public String asString() {
        return type().keyword();
    }

    public enum KeywordType {
      STRUCT("struct"),
      IF("if"),
      ELSE("else"),
      WHILE("while"),
      FOR("for"),
      CONTINUE("continue"),
      BREAK("break"),
      RETURN("return"),
      ASSERT("assert"),
      TRUE("true"),
      FALSE("false"),
      NULL("NULL"),
      PRINT("print"),
      READ("read"),
      FLUSH("flush"),
      ALLOC("alloc"),
      ALLOC_ARRAY("alloc_array"),
      INT("int"),
      BOOL("bool"),
      VOID("void"),
      CHAR("char"),
      STRING("string"),
      ;

      private final String keyword;

      KeywordType(String keyword) {
        this.keyword = keyword;
      }

      public String keyword() {
        return keyword;
      }

      @Override
      public String toString() {
        return keyword();
      }
    }
}
