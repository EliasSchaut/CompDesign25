package edu.kit.kastel.vads.compiler.parser.ast.expression;

import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.parser.ast.NameTree;
import edu.kit.kastel.vads.compiler.parser.visitor.Visitor;
import java.util.List;
import java.util.Objects;

public final class FunctionCallTree
    implements ExpressionTree {
    private NameTree name;
    private List<ExpressionTree> arguments;

    public FunctionCallTree(NameTree name, List<ExpressionTree> arguments) {
        this.name = name;
        this.arguments = arguments;
    }

    @Override
    public Span span() {
        if (arguments.isEmpty()) {
            return name.span();
        }

        return name.span().merge(arguments.getLast().span());
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

    public List<ExpressionTree> arguments() {
        return arguments;
    }

    public void setArguments(List<ExpressionTree> arguments) {
        this.arguments = arguments;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (FunctionCallTree) obj;
        return Objects.equals(this.name, that.name) &&
            Objects.equals(this.arguments, that.arguments);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, arguments);
    }

    @Override
    public String toString() {
        return "FunctionCallTree[" +
            "name=" + name + ", " +
            "arguments=" + arguments + ']';
    }

}
