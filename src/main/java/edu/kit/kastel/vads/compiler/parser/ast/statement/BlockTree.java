package edu.kit.kastel.vads.compiler.parser.ast.statement;

import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.parser.visitor.Visitor;

import java.util.ArrayList;
import java.util.List;

public record BlockTree(List<StatementTree> statements, Span span) implements StatementTree {

    public BlockTree {
        statements = new ArrayList<>(statements);
    }

    @Override
    public <T, R> R accept(Visitor<T, R> visitor, T data) {
        return visitor.visit(this, data);
    }

    public void setStatement(int index, StatementTree statement) {
        if (index < 0 || index >= statements.size()) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + statements.size());
        }
        statements.set(index, statement);
    }

    public void removeStatement(int index) {
        if (index < 0 || index >= statements.size()) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + statements.size());
        }
        statements.remove(index);
    }
}
