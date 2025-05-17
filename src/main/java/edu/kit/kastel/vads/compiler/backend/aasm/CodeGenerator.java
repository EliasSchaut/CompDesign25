package edu.kit.kastel.vads.compiler.backend.aasm;

import edu.kit.kastel.vads.compiler.backend.regalloc.InterferenceGraph;
import edu.kit.kastel.vads.compiler.backend.regalloc.Register;
import edu.kit.kastel.vads.compiler.ir.IrGraph;
import edu.kit.kastel.vads.compiler.ir.analyse.ColoringGraph;
import edu.kit.kastel.vads.compiler.ir.analyse.LivelinessAnalysis;
import edu.kit.kastel.vads.compiler.ir.analyse.MaximumCardinalitySearch;
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

import java.util.*;

import static edu.kit.kastel.vads.compiler.ir.util.NodeSupport.predecessorSkipProj;

public class CodeGenerator {

    public String generateCode(List<IrGraph> graphs, NodeOrderGenerator orderGenerator) {
        var livelinessAnalysis = new LivelinessAnalysis(orderGenerator);
        var livelinessInfo = livelinessAnalysis.analyze();
        var interferenceGraph = new InterferenceGraph(livelinessInfo);
        var simplicialEliminationOrdering = new MaximumCardinalitySearch(interferenceGraph);
        var coloredGraph = new ColoringGraph(interferenceGraph, simplicialEliminationOrdering);
        var registerSpiller = new LeastUsedRegisterSpiller();
        AasmRegisterAllocator allocator = new AasmRegisterAllocator(registerSpiller, coloredGraph);
        StringBuilder builder = new StringBuilder();
        appendPreamble(builder);

        Map<Node, Register> registers = new HashMap<>();
        for (IrGraph graph : graphs) {
            registers.putAll(allocator.allocateRegisters(graph));
        }

        for (Node node : orderGenerator.getOrder()) {
            generateForNode(node, builder, registers);
        }

        return builder.toString();
    }

    private void appendPreamble(StringBuilder builder) {
        builder.append("""
                .section .note-GNU-stack
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

    private void generateForNode(Node node, StringBuilder builder, Map<Node, Register> registers) {
        switch (node) {
            case AddNode add -> binary(builder, registers, add, "add");
            case SubNode sub -> binary(builder, registers, sub, "sub");
            case MulNode mul -> signExtendedBinary(builder, registers, mul, "mulq");
            case DivNode div -> signExtendedBinary(builder, registers, div, "divq");
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

    private static String loadFromStack(
            Register register,
            String loadIntoRegister) {
        return "mov %s, %s\n"
                .formatted(register, loadIntoRegister);
    }

    private static String storeToStack(
            String storeFromRegister,
            Register register) {
        return "mov %s, %s\n"
                .formatted(storeFromRegister, register);
    }

    private static void binary(
            StringBuilder builder,
            Map<Node, Register> registers,
            BinaryOperationNode node,
            String opcode
    ) {
        var left = registers.get(predecessorSkipProj(node, BinaryOperationNode.LEFT));
        var right = registers.get(predecessorSkipProj(node, BinaryOperationNode.RIGHT));
        var destination = registers.get(node);
        var destinationOrFreeHandRegister = destination.isStackVariable()
                ? destination.getFreeHandRegister()
                : destination.toString();

        builder
                // Comment ---
                .append("# %s".formatted(opcode))
                // -----------
                // load right in %rax if needed
                .append(
                        right.isStackVariable()
                                ? loadFromStack(right, "%rax")
                                : ""
                )
                // load left in destination register
                .append("mov ")
                .append(left)
                .append(", ")
                .append(destinationOrFreeHandRegister)
                .append("\n")
                // execute binary operation
                .append(opcode)
                .append(" ")
                .append(right.isStackVariable() ? "%rax" : right)
                .append(", ")
                .append(destinationOrFreeHandRegister)
                .append("\n")
                // store destination if needed
                .append(destination.isStackVariable()
                        ? storeToStack(destinationOrFreeHandRegister, destination)
                        : "");
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
                .append("mov $")
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


    private static void signExtendedBinary(
            StringBuilder builder,
            Map<Node, Register> registers,
            BinaryOperationNode node,
            String opcode
    ) {
        // rdx:rax / registerX
        var writeTo = registers.get(node);
        Register leftOp = registers.get(predecessorSkipProj(node, BinaryOperationNode.LEFT));
        Register rightOp = registers.get(predecessorSkipProj(node, BinaryOperationNode.RIGHT));
        builder
                // Comment ---
                .append("# %s: ".formatted(opcode))
                .append(writeTo)
                .append(" = ")
                .append(leftOp)
                .append(" / ")
                .append(rightOp)
                .append("\n")
                // -----------
                // load left in rax
                .append("mov ")
                .append(leftOp)
                .append(", %rax\n")
                // sign extend rax to rdx
                .append("cltd\n")
                // load from stack if needed
                .append(rightOp.isStackVariable() ? loadFromStack(rightOp, "%rcx") : "")
                // divide with right in rdx
                .append(opcode)
                .append(" ")
                .append(rightOp.isStackVariable() ? "%rcx" : rightOp)
                .append("\n")
                // store result
                .append("mov %rax, ")
                .append(writeTo)
                .append("\n")
                // write to stack if needed
                .append(writeTo.isStackVariable() ? storeToStack("%rax", writeTo) : "");

    }
}
