package edu.kit.kastel.vads.compiler.parser.ast.statement;

import edu.kit.kastel.vads.compiler.parser.ast.Tree;

public sealed interface StatementTree extends Tree
    permits AssignmentTree, BlockTree, DeclarationTree, ReturnTree {
}
