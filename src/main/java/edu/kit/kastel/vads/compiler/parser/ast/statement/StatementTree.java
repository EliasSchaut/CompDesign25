package edu.kit.kastel.vads.compiler.parser.ast.statement;

import edu.kit.kastel.vads.compiler.parser.ast.Tree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.control.ControlTree;

public sealed interface StatementTree extends Tree
    permits AssignmentTree, BlockTree, DeclarationTree, ControlTree {
}
