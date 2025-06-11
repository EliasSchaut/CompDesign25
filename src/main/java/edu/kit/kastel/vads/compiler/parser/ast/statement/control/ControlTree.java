package edu.kit.kastel.vads.compiler.parser.ast.statement.control;

import edu.kit.kastel.vads.compiler.parser.ast.statement.StatementTree;

public sealed interface ControlTree extends StatementTree
    permits BreakTree, ContinueTree, ForTree, IfTree, ReturnTree, WhileTree {
}
