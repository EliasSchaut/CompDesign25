package edu.kit.kastel.vads.compiler.ir.node.binary;

import edu.kit.kastel.vads.compiler.ir.node.block.Block;
import edu.kit.kastel.vads.compiler.ir.node.Node;

public final class MulNode extends BinaryOperationNode {
    public MulNode(Block block, Node left, Node right) {
        super(block, left, right);
    }

    @SuppressWarnings("EqualsDoesntCheckParameterClass") // we do, but not here
    @Override
    public boolean equals(Object obj) {
        return commutativeEquals(this, obj);
    }

    @Override
    public int hashCode() {
        return commutativeHashCode(this);
    }
}
