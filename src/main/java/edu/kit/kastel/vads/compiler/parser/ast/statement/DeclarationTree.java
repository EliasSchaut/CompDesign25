package edu.kit.kastel.vads.compiler.parser.ast.statement;

import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.parser.ast.expression.ExpressionTree;
import edu.kit.kastel.vads.compiler.parser.ast.NameTree;
import edu.kit.kastel.vads.compiler.parser.ast.TypeTree;
import edu.kit.kastel.vads.compiler.parser.visitor.Visitor;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

public final class DeclarationTree implements StatementTree {
    private TypeTree type;
    private NameTree name;
    private @Nullable ExpressionTree initializer;

    public DeclarationTree(TypeTree type, NameTree name, @Nullable ExpressionTree initializer) {
        this.type = type;
        this.name = name;
        this.initializer = initializer;
    }

    @Override
    public Span span() {
        ExpressionTree initializerTree = initializer();
        if (initializerTree != null) {
            return type().span().merge(initializerTree.span());
        }
        return type().span().merge(name().span());
    }

    @Override
    public <T, R> R accept(Visitor<T, R> visitor, T data) {
        return visitor.visit(this, data);
    }

    public TypeTree type() {
        return type;
    }

    public void setType(TypeTree typeTree) {
        this.type = typeTree;
    }

    public NameTree name() {
        return name;
    }

    public void setName(NameTree nameTree) {
        this.name = nameTree;
    }

    public @Nullable ExpressionTree initializer() {
        return initializer;
    }

    public void setInitializer(@Nullable ExpressionTree initializer) {
        this.initializer = initializer;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (DeclarationTree) obj;
        return Objects.equals(this.type, that.type) &&
            Objects.equals(this.name, that.name) &&
            Objects.equals(this.initializer, that.initializer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, name, initializer);
    }

    @Override
    public String toString() {
        return "DeclarationTree[" +
            "type=" + type + ", " +
            "name=" + name + ", " +
            "initializer=" + initializer + ']';
    }

}
