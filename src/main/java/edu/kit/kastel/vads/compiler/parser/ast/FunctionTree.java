package edu.kit.kastel.vads.compiler.parser.ast;

import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.parser.ast.statement.BlockTree;
import edu.kit.kastel.vads.compiler.parser.visitor.Visitor;
import java.util.List;
import java.util.Objects;

public final class FunctionTree implements Tree {
    private TypeTree returnType;
    private NameTree name;
    private BlockTree body;
    private final List<ParameterTree> parameters;

    public FunctionTree(TypeTree returnType, NameTree name, BlockTree body, List<ParameterTree> parameters) {
        this.returnType = returnType;
        this.name = name;
        this.body = body;
        this.parameters = parameters;
    }

    @Override
    public Span span() {
        return new Span.SimpleSpan(returnType().span().start(), body().span().end());
    }

    @Override
    public <T, R> R accept(Visitor<T, R> visitor, T data) {
        return visitor.visit(this, data);
    }

    public TypeTree returnType() {
        return returnType;
    }

    public void setReturnType(TypeTree returnType) {
        this.returnType = returnType;
    }

    public NameTree name() {
        return name;
    }

    public void setName(NameTree name) {
        this.name = name;
    }

    public BlockTree body() {
        return body;
    }

    public void setBody(BlockTree body) {
        this.body = body;
    }

    public List<ParameterTree> parameters() {
        return parameters;
    }

    public void addParameter(ParameterTree parameter) {
        this.parameters.add(parameter);
    }

    public void removeParameter(ParameterTree parameter) {
        this.parameters.remove(parameter);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (FunctionTree) obj;
        return Objects.equals(this.returnType, that.returnType) &&
            Objects.equals(this.name, that.name) &&
            Objects.equals(this.body, that.body);
    }

    @Override
    public int hashCode() {
        return Objects.hash(returnType, name, body);
    }

    @Override
    public String toString() {
        return "FunctionTree[" +
            "returnType=" + returnType + ", " +
            "name=" + name + ", " +
            "body=" + body + ", " +
            "parameters=" + parameters + ']';
    }

}
