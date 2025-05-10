package edu.kit.kastel.vads.compiler.backend.aasm;

import edu.kit.kastel.vads.compiler.backend.regalloc.Register;
import edu.kit.kastel.vads.compiler.ir.IrGraph;
import edu.kit.kastel.vads.compiler.ir.node.AddNode;
import edu.kit.kastel.vads.compiler.ir.node.BinaryOperationNode;
import edu.kit.kastel.vads.compiler.ir.node.Block;
import edu.kit.kastel.vads.compiler.ir.node.ConstIntNode;
import edu.kit.kastel.vads.compiler.ir.node.DivNode;
import edu.kit.kastel.vads.compiler.ir.node.ModNode;
import edu.kit.kastel.vads.compiler.ir.node.MulNode;
import edu.kit.kastel.vads.compiler.ir.node.Node;
import edu.kit.kastel.vads.compiler.ir.node.Phi;
import edu.kit.kastel.vads.compiler.ir.node.ProjNode;
import edu.kit.kastel.vads.compiler.ir.node.ReturnNode;
import edu.kit.kastel.vads.compiler.ir.node.StartNode;
import edu.kit.kastel.vads.compiler.ir.node.SubNode;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static edu.kit.kastel.vads.compiler.ir.util.NodeSupport.predecessorSkipProj;

public class CodeGenerator {

    public String generateCode(List<IrGraph> program) {
        StringBuilder builder = new StringBuilder();
        for (IrGraph graph : program) {
            AasmRegisterAllocator allocator = new AasmRegisterAllocator();
            Map<Node, Register> registers = allocator.allocateRegisters(graph);
            appendPreamble(builder);
            generateForGraph(graph, builder, registers);
            builder.append("\n");
        }
        return builder.toString();
    }

    private void appendPreamble(StringBuilder builder) {
        builder.append("""
                .global main
                .global _main
                .text
                
                main:
                call _main
                movq %rax, %rdi
                movq $0x3C, %rax
                syscall
                
                _main:
                """);
    }

    private void generateForGraph(IrGraph graph, StringBuilder builder, Map<Node, Register> registers) {
        Set<Node> visited = new HashSet<>();
        scan(graph.endBlock(), visited, builder, registers);
    }

    private void scan(Node node, Set<Node> visited, StringBuilder builder, Map<Node, Register> registers) {
        for (Node predecessor : node.predecessors()) {
            if (visited.add(predecessor)) {
                scan(predecessor, visited, builder, registers);
            }
        }

        switch (node) {
            case AddNode add -> binary(builder, registers, add, "add");
            case SubNode sub -> binary(builder, registers, sub, "sub");
            case MulNode mul -> multiply(builder, registers, mul, "mulq");
            case DivNode div -> divide(builder, registers, div, "divq");
            case ModNode mod -> binary(builder, registers, mod, "mod");
            case ReturnNode r -> returnNode(builder, registers, r);
            case ConstIntNode c -> loadConst(builder, registers, c);
            case Phi _ -> throw new UnsupportedOperationException("phi");
            case Block _, ProjNode _, StartNode _ -> {
                // do nothing, skip line break
                return;
            }
        }
        builder.append("\n");
    }

    private static void binary(
            StringBuilder builder,
            Map<Node, Register> registers,
            BinaryOperationNode node,
            String opcode
    ) {
        builder
                .append("mov ")
                .append(registers.get(predecessorSkipProj(node, BinaryOperationNode.RIGHT)))
                .append(", ")
                .append(registers.get(node))
                .append("\n")
                .append(opcode)
                .append(" ")
                .append(registers.get(predecessorSkipProj(node, BinaryOperationNode.LEFT)))
                .append(", ")
                .append(registers.get(node))
                .append("\n");
    }

    private static void loadConst(
            StringBuilder builder,
            Map<Node, Register> registers,
            ConstIntNode c
    ) {
        builder
                .append("# load const: ")
                .append(registers.get(c))
                .append("\n")
                .append("mov $0x")
                .append(c.value())
                .append(", ")
                .append(registers.get(c))
                .append("\n");
    }

    private static void returnNode(
            StringBuilder builder,
            Map<Node, Register> registers,
            ReturnNode r
    ) {
        builder
                .append("mov ")
                .append(registers.get(predecessorSkipProj(r, ReturnNode.RESULT)))
                .append(", %rax\n")
                .append("ret")
                .append("\n");
    }

    private static void multiply(
            StringBuilder builder,
            Map<Node, Register> registers,
            BinaryOperationNode node,
            String opcode
    ) {
        builder
                .append("# multiply: ")
                .append(registers.get(predecessorSkipProj(node, BinaryOperationNode.LEFT)))
                .append(" * ")
                .append(registers.get(predecessorSkipProj(node, BinaryOperationNode.RIGHT)))
                .append("\n")
                .append("mov $0x0, %rdx\n")
                .append("mov ")
                .append(registers.get(predecessorSkipProj(node, BinaryOperationNode.LEFT)))
                .append(", %rax\n")
                .append(opcode)
                .append(" ")
                .append(registers.get(predecessorSkipProj(node, BinaryOperationNode.RIGHT)))
                .append("\n")
                .append("mov %rax, ")
                .append(registers.get(node))
                .append("\n");
    }

    private static void divide(
            StringBuilder builder,
            Map<Node, Register> registers,
            BinaryOperationNode node,
            String opcode
    ) {
        // rdx:rax / registerX
        builder
                .append("# divide: ")
                .append(registers.get(predecessorSkipProj(node, BinaryOperationNode.LEFT)))
                .append(" / ")
                .append(registers.get(node))
                .append("\n")
                .append("mov $0x0, %rdx\n")
                .append("mov ")
                .append(registers.get(predecessorSkipProj(node, BinaryOperationNode.LEFT)))
                .append(", %rax\n")
                .append(opcode)
                .append(" ")
                .append(registers.get(predecessorSkipProj(node, BinaryOperationNode.RIGHT)))
                .append("\n")
                .append("mov %rax, ")
                .append(registers.get(node))
                .append("\n");
    }
}
