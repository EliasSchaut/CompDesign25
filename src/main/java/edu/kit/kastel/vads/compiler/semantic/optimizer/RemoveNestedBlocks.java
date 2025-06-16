package edu.kit.kastel.vads.compiler.semantic.optimizer;

import edu.kit.kastel.vads.compiler.parser.ast.Tree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.BlockTree;
import edu.kit.kastel.vads.compiler.parser.visitor.NodeReplacementVisitor;
import edu.kit.kastel.vads.compiler.parser.visitor.Unit;

public class RemoveNestedBlocks implements NodeReplacementVisitor<Unit> {

    @Override
    public Tree visit(BlockTree blockTree, Unit data) {
        // If the block has only one child and that child is also a block,
        // we can remove the outer block and return the inner block's content.
        if (blockTree.statements().size() == 1 &&
            blockTree.statements().getFirst() instanceof BlockTree innerBlock) {
            return visit(innerBlock, data);
        }
        return blockTree;
    }
}
