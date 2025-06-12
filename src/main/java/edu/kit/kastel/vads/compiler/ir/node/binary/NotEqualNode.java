 package edu.kit.kastel.vads.compiler.ir.node.binary;

 import edu.kit.kastel.vads.compiler.ir.node.Node;
 import edu.kit.kastel.vads.compiler.ir.node.block.Block;

 public final class NotEqualNode extends BinaryOperationNode {
    public NotEqualNode(Block block, Node left, Node right) {
        super(block, left, right);
    }
}
