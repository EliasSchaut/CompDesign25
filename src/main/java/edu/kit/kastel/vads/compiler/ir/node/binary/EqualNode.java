 package edu.kit.kastel.vads.compiler.ir.node.binary;

 import edu.kit.kastel.vads.compiler.ir.node.Node;
 import edu.kit.kastel.vads.compiler.ir.node.block.Block;

 public final class EqualNode extends BinaryOperationNode {
    public EqualNode(Block block, Node left, Node right) {
        super(block, left, right);
    }
}
