package edu.kit.kastel.vads.compiler.backend.aasm;

import edu.kit.kastel.vads.compiler.backend.regalloc.InterferenceGraph;
import edu.kit.kastel.vads.compiler.backend.regalloc.Register;
import edu.kit.kastel.vads.compiler.ir.IrGraph;
import edu.kit.kastel.vads.compiler.ir.analyse.ColoringGraph;
import edu.kit.kastel.vads.compiler.ir.analyse.LivelinessAnalysis;
import edu.kit.kastel.vads.compiler.ir.analyse.MaximumCardinalitySearch;
import edu.kit.kastel.vads.compiler.ir.node.binary.AddNode;
import edu.kit.kastel.vads.compiler.ir.node.binary.AndNode;
import edu.kit.kastel.vads.compiler.ir.node.binary.BinaryOperationNode;
import edu.kit.kastel.vads.compiler.ir.node.binary.BitwiseAndNode;
import edu.kit.kastel.vads.compiler.ir.node.binary.BitwiseOrNode;
import edu.kit.kastel.vads.compiler.ir.node.binary.GreaterEqualNode;
import edu.kit.kastel.vads.compiler.ir.node.binary.LessEqualNode;
import edu.kit.kastel.vads.compiler.ir.node.block.Block;
import edu.kit.kastel.vads.compiler.ir.node.constant.ConstBoolNode;
import edu.kit.kastel.vads.compiler.ir.node.constant.ConstIntNode;
import edu.kit.kastel.vads.compiler.ir.node.binary.DivNode;
import edu.kit.kastel.vads.compiler.ir.node.binary.GreaterNode;
import edu.kit.kastel.vads.compiler.ir.node.binary.LessNode;
import edu.kit.kastel.vads.compiler.ir.node.binary.ModNode;
import edu.kit.kastel.vads.compiler.ir.node.binary.MulNode;
import edu.kit.kastel.vads.compiler.ir.node.Node;
import edu.kit.kastel.vads.compiler.ir.node.binary.OrNode;
import edu.kit.kastel.vads.compiler.ir.node.Phi;
import edu.kit.kastel.vads.compiler.ir.node.block.ProjNode;
import edu.kit.kastel.vads.compiler.ir.node.block.ReturnNode;
import edu.kit.kastel.vads.compiler.ir.node.binary.ShiftLeftNode;
import edu.kit.kastel.vads.compiler.ir.node.binary.ShiftRightNode;
import edu.kit.kastel.vads.compiler.ir.node.block.StartNode;
import edu.kit.kastel.vads.compiler.ir.node.binary.SubNode;

import edu.kit.kastel.vads.compiler.ir.node.binary.XorNode;
import edu.kit.kastel.vads.compiler.ir.node.unary.UnaryOperationNode;
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

        Map<Node, Register> registers = new HashMap<>();
        for (IrGraph graph : graphs) {
            registers.putAll(allocator.allocateRegisters(graph));
        }

        // Write code
        StringBuilder builder = new StringBuilder();
        appendPreamble(builder, allocator.getStackSize());

        for (Node node : orderGenerator.getOrder()) {
            generateForNode(node, builder, registers);
        }

        return builder.toString();
    }

    private void appendPreamble(StringBuilder builder, int stackSize) {
        builder.append("""
                .section .note-GNU-stack
                .global main
                .global _main
                .text
                
                main:
                # Allocate %d bytes for local variables
                sub $%d, %%rsp
                
                call _main
                
                # Deallocate %d bytes for local variables
                add $%d, %%rsp
                
                # Exit program
                mov %%eax, %%edi
                mov $0x3C, %%eax
                syscall
                
                _main:
                """.formatted(stackSize, stackSize, stackSize, stackSize));
    }

    private void generateForNode(Node node, StringBuilder builder, Map<Node, Register> registers) {
        switch (node) {
            case AddNode add -> binary(builder, registers, add, "add");
            case SubNode sub -> binary(builder, registers, sub, "sub");
            case MulNode mul -> signExtendedBinary(builder, registers, mul, "*", "mull", "%eax");
            case DivNode div -> signExtendedBinary(builder, registers, div, "/", "idivl", "%eax");
            case ModNode mod -> signExtendedBinary(builder, registers, mod, "%", "idivl", "%edx");
            case ReturnNode r -> returnNode(builder, registers, r);
            case ConstIntNode c -> loadConst(builder, registers, c);
            case Phi _ -> throw new UnsupportedOperationException("phi");
            case Block _, ProjNode _, StartNode _ -> {
                // do nothing, skip line break
                return;
            }
            case AndNode andNode -> {
            }
            case GreaterNode greaterNode -> {
            }
            case LessNode lessNode -> {
            }
            case OrNode orNode -> {
            }
            case ShiftLeftNode shiftLeftNode -> {
            }
            case ShiftRightNode shiftRightNode -> {
            }
            case XorNode xorNode -> {
            }
            case BitwiseAndNode bitwiseAndNode -> {
            }
            case BitwiseOrNode bitwiseOrNode -> {
            }
            case GreaterEqualNode greaterEqualNode -> {
            }
            case LessEqualNode lessEqualNode -> {
            }
            case ConstBoolNode constBoolNode -> {
            }
            case UnaryOperationNode unaryOperationNode -> {
            }
        }
        builder.append("\n");
    }

    private static String loadFromStack(
            Register register,
            String loadIntoRegister) {
        return "movl %s, %s\n"
                .formatted(register, loadIntoRegister);
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
        boolean destinationIsOnStack = destination.isStackVariable();
        boolean destinationIsRight = destination.toString().equals(right.toString());
        boolean useFreeHandRegister = destinationIsOnStack || destinationIsRight;
        var resultRegister = useFreeHandRegister
            ? destination.getFreeHandRegister()
            : destination.toString();

         /*
         Special cases:

         a = b - a
           > c = b
           > c -= a
           > a = c

         a = a - b
           > a -= b
          */

        builder
                // Comment ---
                .append("# %s = %s %s %s\n"
                    .formatted(destination, left, opcode.equals("sub") ? "-" : "+", right))
                // -----------
                // load right in %eax if needed
                .append(right.isStackVariable()
                    ? loadFromStack(right, "%eax")
                    : ""
                )
                // load left in destination register if left is not the same as destination
                .append(left.toString().equals(resultRegister)
                    ? ""
                    : "movl %s, %s\n"
                    .formatted(left, resultRegister)
                )
                // execute binary operation
                .append(opcode)
                .append(" ")
                .append(right.isStackVariable() ? "%eax" : right)
                .append(", ")
                .append(resultRegister)
                .append("\n")
                // move result to destination if result was written in intermediate register
                .append(useFreeHandRegister
                    ? "movl %s, %s\n".formatted(resultRegister, destination)
                    : "");
    }

    private static void loadConst(
            StringBuilder builder,
            Map<Node, Register> registers,
            ConstIntNode c
    ) {
        builder
                // Comment ---
                .append("# load const: ")
                .append(c.value())
                .append("\n")
                // -----------
                .append("movl $")
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
                .append("movl ")
                .append(registers.get(predecessorSkipProj(r, ReturnNode.RESULT)))
                .append(", %eax\n")
                .append("ret")
                .append("\n");
    }


    private static void signExtendedBinary(
            StringBuilder builder,
            Map<Node, Register> registers,
            BinaryOperationNode node,
            String operation,
            String opcode,
            String outputRegister
    ) {
        var writeTo = registers.get(node);
        Register leftOp = registers.get(predecessorSkipProj(node, BinaryOperationNode.LEFT));
        Register rightOp = registers.get(predecessorSkipProj(node, BinaryOperationNode.RIGHT));
        builder
                // Comment ---
                .append("# %s: ".formatted(opcode))
                .append(writeTo)
                .append(" = ")
                .append(leftOp)
                .append(" ")
                .append(operation)
                .append(" ")
                .append(rightOp)
                .append("\n")
                // -----------
                // load left in eax
                .append("movl ")
                .append(leftOp)
                .append(", %eax\n")
                // sign extend eax to edx
                .append("cdq\n")
                // load from stack if needed
                .append(rightOp.isStackVariable() ? loadFromStack(rightOp, "%ecx") : "")
                // divide with right in rdx
                .append(opcode)
                .append(" ")
                .append(rightOp.isStackVariable() ? "%ecx" : rightOp)
                .append("\n")
                // store result
                .append("movl ")
                .append(outputRegister)
                .append(", ")
                .append(writeTo)
                .append("\n");

    }
}
