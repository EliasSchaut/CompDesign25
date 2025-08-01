package edu.kit.kastel.vads.compiler.parser.ast.expression;

import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.parser.ast.NameTree;
import edu.kit.kastel.vads.compiler.parser.visitor.Visitor;
import java.util.Objects;

public final class IdentExpressionTree implements ExpressionTree {
    private NameTree name;

    public IdentExpressionTree(NameTree name) {
        this.name = name;
    }

    @Override
    public Span span() {
        return name().span();
    }

    @Override
    public <T, R> R accept(Visitor<T, R> visitor, T data) {
        return visitor.visit(this, data);
    }

    public NameTree name() {
        return name;
    }

    public void setName(NameTree name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (IdentExpressionTree) obj;
        return Objects.equals(this.name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "IdentExpressionTree[" +
            "name=" + name + ']';
    }

}
