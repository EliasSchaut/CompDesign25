package edu.kit.kastel.vads.compiler.parser.ast;

import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.parser.visitor.Visitor;

public final class ParameterTree implements Tree {
    private TypeTree type;
    private NameTree name;

    public ParameterTree(TypeTree type, NameTree name) {
        this.type = type;
        this.name = name;
    }

    @Override
    public Span span() {
        return new Span.SimpleSpan(type.span().start(), name.span().end());
    }

    @Override
    public <T, R> R accept(Visitor<T, R> visitor, T data) {
        return visitor.visit(this, data);
    }

    public TypeTree type() {
        return type;
    }

    public void setType(TypeTree type) {
        this.type = type;
    }

    public NameTree name() {
        return name;
    }

    public void setName(NameTree name) {
        this.name = name;
    }
}
