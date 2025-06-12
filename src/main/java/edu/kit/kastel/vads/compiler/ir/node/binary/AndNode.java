 package edu.kit.kastel.vads.compiler.ir.node.binary;

 import edu.kit.kastel.vads.compiler.ir.node.block.Block;
 import edu.kit.kastel.vads.compiler.ir.node.Node;

 public final class AndNode extends BinaryOperationNode {
    public AndNode(Block block, Node left, Node right) {
        super(block, left, right);
    }
}
