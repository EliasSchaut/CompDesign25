package edu.kit.kastel.vads.compiler.backend.aasm;

import static edu.kit.kastel.vads.compiler.ir.util.NodeSupport.predecessorSkipProj;

import edu.kit.kastel.vads.compiler.backend.regalloc.AasmRegisterAllocator;
import edu.kit.kastel.vads.compiler.backend.regalloc.Register;
import edu.kit.kastel.vads.compiler.backend.regalloc.RegisterAllocator;
import edu.kit.kastel.vads.compiler.backend.regalloc.SimpleAasmRegisterAllocator;
import edu.kit.kastel.vads.compiler.backend.regalloc.VirtualRegister;
import edu.kit.kastel.vads.compiler.ir.IrGraph;
import edu.kit.kastel.vads.compiler.ir.node.Node;
import edu.kit.kastel.vads.compiler.ir.node.Phi;
import edu.kit.kastel.vads.compiler.ir.node.binary.*;
import edu.kit.kastel.vads.compiler.ir.node.block.*;
import edu.kit.kastel.vads.compiler.ir.node.constant.ConstBoolNode;
import edu.kit.kastel.vads.compiler.ir.node.constant.ConstIntNode;
import edu.kit.kastel.vads.compiler.ir.node.control.IfNode;
import edu.kit.kastel.vads.compiler.ir.node.control.TernaryNode;
import edu.kit.kastel.vads.compiler.ir.node.unary.BitwiseNotNode;
import edu.kit.kastel.vads.compiler.ir.node.unary.NotNode;
import edu.kit.kastel.vads.compiler.ir.node.unary.UnaryMinusNode;
import edu.kit.kastel.vads.compiler.ir.node.unary.UnaryOperationNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CodeGenerator {
    private static final String EXTRA_STATEMENTS = "(╬▔皿▔)╯";

    private record StringBuilderWithBlockName(String blockName, StringBuilder builder) {
    }

    private final Map<String, List<String>> extraStatementsInBlockBeforeJump = new HashMap<>();

    public String generateCode(List<IrGraph> graphs) {
        List<Map<Node, Register>> registerAllocations = new ArrayList<>();
        boolean dontReuseRegisters = true;
        RegisterAllocator allocator = dontReuseRegisters
            ? new SimpleAasmRegisterAllocator()
            : new AasmRegisterAllocator();

        List<StringBuilderWithBlockName> blockBuilders = new ArrayList<>();
        for (IrGraph graph : graphs) {
            var orderGenerator = new NodeOrderGenerator(graph);

            var registers = allocator.allocateRegisters(orderGenerator);
            registerAllocations.add(registers);

            for (NodeOrderGenerator.OrderedBlock orderedBlock : orderGenerator.getOrder()) {
                StringBuilder blockBuilder = new StringBuilder();
                blockBuilder
                    // Comment ---
                    .append("# --- ")
                    .append(orderedBlock.blockName())
                    .append(" ---\n")
                    // -----------
                    .append(orderedBlock.blockName())
                    .append(":\n");

                for (Node node : orderedBlock.nodes()) {
                    generateForNode(node, blockBuilder, registers);
                }

                blockBuilders.add(new StringBuilderWithBlockName(orderedBlock.blockName(), blockBuilder));
            }
        }

        // Write code
        StringBuilder builder = new StringBuilder();

        var maxStackRegisters = registerAllocations
            .stream()
            .map(allocation -> allocation.values().stream()
                .filter(Register::isStackVariable)
                .mapToInt(_ -> 1)
                .sum())
            .max(Integer::compare)
            .orElseThrow();

        addPreamble(builder, maxStackRegisters * VirtualRegister.REGISTER_BYTE_SIZE);

        // Write blocks
        for (StringBuilderWithBlockName blockBuilder : blockBuilders) {
            List<String> strings = extraStatementsInBlockBeforeJump.get(blockBuilder.blockName());
            if (strings == null) {
                var blockString = blockBuilder.builder().toString().replace(EXTRA_STATEMENTS, "");
                builder.append(blockString);
            } else {
                var allExtraStrings = String.join("\n", strings);
                String blockString = blockBuilder.builder().toString();
                if (!blockString.contains(EXTRA_STATEMENTS)) {
                    throw new IllegalStateException(
                        "Block string for " + blockBuilder.blockName() +
                            " does not contain the EXTRA_STATEMENTS placeholder but received extra statements: " +
                            allExtraStrings);
                }
                var blockWithExtraStrings = blockString.replace(EXTRA_STATEMENTS, allExtraStrings);

                builder.append(blockWithExtraStrings);
            }
        }

        return builder.toString();
    }

    private void addPreamble(StringBuilder builder, int stackSize) {
        builder.append("""
                .section .note.GNU-stack
                .global main
                .global start
                .text

                main:
                # Allocate %d bytes for local variables
                sub $%d, %%rsp

                call start

                # Deallocate %d bytes for local variables
                add $%d, %%rsp

                # Exit program
                mov %%eax, %%edi
                mov $0x3C, %%eax
                syscall
                
                # ----- Start Program -----
                
                """.formatted(stackSize, stackSize, stackSize, stackSize));
    }

    private void generateForNode(Node node, StringBuilder builder, Map<Node, Register> registers) {
        switch (node) {
            // constants
            case ConstIntNode c -> loadConstInt(builder, registers, c);
            case ConstBoolNode b -> loadConstBool(builder, registers, b);

            // arithmetic
            case AddNode add -> binary(builder, registers, add, "add");
            case SubNode sub -> binary(builder, registers, sub, "sub");
            case MulNode mul -> signExtendedBinary(builder, registers, mul, "*", "mull", "%eax");
            case DivNode div -> signExtendedBinary(builder, registers, div, "/", "idivl", "%eax");
            case ModNode mod -> signExtendedBinary(builder, registers, mod, "%", "idivl", "%edx");
            case ShiftLeftNode shiftLeft -> binary(builder, registers, shiftLeft, "sall");
            case ShiftRightNode shiftRight -> binary(builder, registers, shiftRight, "sarl");

            // unary logical
            case NotNode not -> logicalNot(builder, registers, not);
            case BitwiseNotNode bitwiseNot -> unary(builder, registers, bitwiseNot, "not");
            case UnaryMinusNode unaryMinus -> unary(builder, registers, unaryMinus, "negl");

            // binary logical
            case EqualNode equal -> compare(builder, registers, equal, "==");
            case NotEqualNode notEqual -> compare(builder, registers, notEqual, "!=");
            case BitwiseAndNode bitwiseAnd -> binary(builder, registers, bitwiseAnd, "and");
            case BitwiseOrNode bitwiseOr -> binary(builder, registers, bitwiseOr, "or");
            case XorNode xor -> binary(builder, registers, xor, "xor");
            case GreaterNode greater -> compare(builder, registers, greater, ">");
            case LessNode less -> compare(builder, registers, less, "<");
            case GreaterEqualNode greaterEqual -> compare(builder, registers, greaterEqual, ">=");
            case LessEqualNode lessEqual -> compare(builder, registers, lessEqual, "<=");

            // control flow
            case JumpNode jump -> controlJump(builder, registers, jump);
            case IfNode ifNode -> controlIf(builder, registers, ifNode);
            case TernaryNode ternaryNode -> ternary(builder, registers, ternaryNode);
            case ReturnNode r -> returnNode(builder, registers, r);
            case Phi phi -> {
                boolean onlySideEffects = phi
                    .predecessors()
                    .stream()
                    .allMatch(
                        pred -> pred instanceof ProjNode && ((ProjNode) pred)
                            .projectionInfo() == ProjNode.SimpleProjectionInfo.SIDE_EFFECT);

                if (onlySideEffects) {
                    break;
                }

                Set<Node> visited = new HashSet<>();
                var phiJumps = phi.block().predecessors();
                var phiValues = new ArrayList<Node>();
                List<? extends Node> predecessors = phi.predecessors();
                for (int i = 0; i < predecessors.size(); i++) {
                    phiValues.add(predecessorSkipProj(phi, i));
                }

                if (phiJumps.size() != phiValues.size()) {
                    throw new IllegalStateException(
                        "Phi node %s has %d jumps but %d values".formatted(phi, phiJumps.size(),
                            phiValues.size()));
                }

                for (int i = 0; i < phiJumps.size(); i++) {
                    if (visited.add(phiJumps.get(i))) {
                        Node pred = phiValues.get(i);
                        Block blockPred = phiJumps.get(i).block();

                        List<String> extraStatements = extraStatementsInBlockBeforeJump
                            .computeIfAbsent(blockPred.name(), _ -> new ArrayList<>());

                        var extraBuilder = new StringBuilder();
                        var source = registers.get(pred);
                        if (source == null) {
                            // TODO: Check if this is really not a problem
                            continue;
                        }

                        var destination = getRegister(registers, phi);
                        extraBuilder
                                // Comment ---
                                .append("# phi %s from %s\n".formatted(destination, blockPred.name()))
                                // -----------
                                .append(source.isStackVariable()
                                    ? loadFromStack(source, source.getFreeHandRegister())
                                    : ""
                                )
                                .append("movl ")
                                .append(source.isStackVariable()
                                    ? source.getFreeHandRegister()
                                    : source)
                                .append(", ")
                                .append(destination)
                                .append("\n");

                        extraStatements.add(extraBuilder.toString());
                    }
                }
            }
            case Block _, ProjNode _, StartNode _ -> {
                // do nothing, skip line break
                return;
            }
        }
        builder.append("\n");
    }

    private void controlJump(
            StringBuilder builder,
            Map<Node, Register> registers,
            JumpNode jumpNode
    ) {
        var target = jumpNode.getTarget();
        builder
                // Extra statements before jump
                .append(EXTRA_STATEMENTS + "\n\n")
                // Comment ---
                .append("# jump to ")
                .append(target.name())
                .append("\n")
                // -----------
                .append("jmp ")
                .append(target.name())
                .append("\n");
    }

    private void ternary(
        StringBuilder builder,
        Map<Node, Register> registers,
        TernaryNode ternaryNode
    ) {
        var condition = getRegister(registers, predecessorSkipProj(ternaryNode, TernaryNode.CONDITION));
        var thenBlock = ternaryNode.getThenExpr();
        var elseBlock = ternaryNode.getElseExpr();

        builder
            // Comment ---
            .append(
                "# ternary %s ? %s : %s\n".formatted(condition, thenBlock.name(), elseBlock.name()))
            // -----------
            .append(EXTRA_STATEMENTS + "\n\n")
            .append("cmpl $1, %s\n".formatted(condition))
            .append("je %s\n".formatted(thenBlock.name()))
            .append("jmp %s\n\n".formatted(elseBlock.name()));
    }

    private void controlIf(
            StringBuilder builder,
            Map<Node, Register> registers,
            IfNode ifNode
    ) {
        var condition = getRegister(registers, predecessorSkipProj(ifNode, IfNode.CONDITION));
        var thenBlock = ifNode.getThenBlock();
        var elseBlock = ifNode.getElseBlock();

        builder
                // Comment ---
                .append("# if ")
                .append(condition)
                .append("\n")
                // -----------
                .append(EXTRA_STATEMENTS + "\n\n")
                .append("cmpl $1, %s\n".formatted(condition))
                .append("je %s\n".formatted(thenBlock.name()))
                .append("jmp %s\n\n".formatted(elseBlock.name()));
    }

    private static void logicalNot(
        StringBuilder builder,
        Map<Node, Register> registers,
        NotNode not) {
        var destination = getRegister(registers, not);
        var resultRegister = destination.isStackVariable()
            ? destination.getFreeHandRegister()
            : destination.toString();
        var operand = getRegister(registers, predecessorSkipProj(not, UnaryOperationNode.OPERANT));
        builder
            // Comment ---
            .append("# logical NOT: !")
            .append(operand)
            .append("\n")
            // -----------
            // Compare operand with 0
            .append("cmpl $0, ")
            .append(operand)
            .append("\n")
            // Set result to 1 if operand is 0, otherwise 0
            .append("sete %al\n")
            .append("movzx %al, ")
            .append(resultRegister)
            .append("\n")

            // move result to destination if result was written in intermediate register
            .append(destination.isStackVariable()
                ? "movl %s, %s\n".formatted(resultRegister, destination)
                : "");
    }

    private static void unary(
            StringBuilder builder,
            Map<Node, Register> registers,
            UnaryOperationNode node,
            String opcode
    ) {
        var destination = getRegister(registers, node);
        var operand = getRegister(registers, predecessorSkipProj(node, UnaryOperationNode.OPERANT));
        boolean destinationIsOnStack = destination.isStackVariable();
        boolean useFreeHandRegister = destinationIsOnStack || destination.toString().equals(operand.toString());
        var resultRegister = useFreeHandRegister
                ? destination.getFreeHandRegister()
                : destination.toString();

        builder
                // Comment ---
                .append("# %s %s\n".formatted(opcode, operand))
                // -----------
                // load operand in destination register if operand is not the same as destination
                .append(operand.toString().equals(resultRegister)
                        ? ""
                        : "movl %s, %s\n"
                        .formatted(operand, resultRegister)
                )
                // execute unary operation
                .append(opcode)
                .append(" ")
                .append(resultRegister)
                .append("\n")
                // move result to destination if result was written in intermediate register
                .append(useFreeHandRegister
                        ? "movl %s, %s\n".formatted(resultRegister, destination)
                        : "");
    }

    private static void compare(
            StringBuilder builder,
            Map<Node, Register> registers,
            BinaryOperationNode node,
            String opcode
    ) {
        var left = getRegister(registers, predecessorSkipProj(node, BinaryOperationNode.LEFT));
        var right = getRegister(registers, predecessorSkipProj(node, BinaryOperationNode.RIGHT));
        var destination = getRegister(registers, node);
        boolean destinationIsOnStack = destination.isStackVariable();
        boolean destinationIsRight = destination.toString().equals(right.toString());
        boolean useFreeHandRegister = destinationIsOnStack || destinationIsRight;
        var resultRegister = useFreeHandRegister
                ? destination.getFreeHandRegister()
                : destination.toString();
        var set_opcode = switch (opcode) {
            case ">" -> "setg";
            case "<" -> "setl";
            case ">=" -> "setge";
            case "<=" -> "setle";
            case "==" -> "sete";
            case "!=" -> "setne";
            default -> throw new IllegalArgumentException("Unknown comparison opcode: " + opcode);
        };

        builder
                // Comment ---
                .append("# %s %s %s\n".formatted(left, opcode, right))
                // -----------
                // load right in %eax if needed
                .append(right.isStackVariable()
                        ? loadFromStack(right, "%eax")
                        : ""
                )
                // load left in destination register if left is different from destination
                .append(left.toString().equals(resultRegister)
                        ? ""
                        : "movl %s, %s\n"
                        .formatted(left, resultRegister)
                )
                // execute binary operation
                .append("cmpl ")
                .append(right.isStackVariable() ? "%eax" : right)
                .append(", ")
                .append(resultRegister)
                .append("\n")
                // set result in destination register
                .append(set_opcode)
                .append(" %al\n")
                .append("movzx %al, ")
                .append(resultRegister)
                .append("\n")

                // move result to destination if result was written in intermediate register
                .append(useFreeHandRegister
                        ? "movl %s, %s\n".formatted(resultRegister, destination)
                        : "");
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
        var left = getRegister(registers, predecessorSkipProj(node, BinaryOperationNode.LEFT));
        var right = getRegister(registers, predecessorSkipProj(node, BinaryOperationNode.RIGHT));
        var destination = getRegister(registers, node);
        boolean destinationIsOnStack = destination.isStackVariable();
        boolean destinationIsRight = destination.toString().equals(right.toString());
        boolean useDifferentRegister = destinationIsOnStack || destinationIsRight;
        var resultRegister = useDifferentRegister
                ? "%eax"
                : destination.toString();
        boolean isShift = opcode.equals("sall") || opcode.equals("sarl");

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
                        .formatted(destination, left, opcode, right))
                // -----------
                // load right in free hand if needed
                .append(
                    isShift
                        // Load into CL register
                        ? "movl %s, %%ecx\n".formatted(right)
                        : right.isStackVariable()
                            ? loadFromStack(right, right.getFreeHandRegister())
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
                .append(
                    isShift
                        // Shift with CL register
                        ? "%cl"
                        : right.isStackVariable()
                            ? right.getFreeHandRegister()
                            : right)
                .append(", ")
                .append(resultRegister)
                .append("\n")
                // move result to destination if result was written in intermediate register
                .append(useDifferentRegister
                        ? "movl %s, %s\n".formatted(resultRegister, destination)
                        : "");
    }

    private static void loadConstInt(
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
                .append(getRegister(registers, c))
                .append("\n");
    }

    private static void loadConstBool(
            StringBuilder builder,
            Map<Node, Register> registers,
            ConstBoolNode b
    ) {
        builder
                // Comment ---
                .append("# load const: ")
                .append(b.value())
                .append("\n")
                // -----------
                .append("movl $")
                .append(b.value() ? 1 : 0)
                .append(", ")
                .append(getRegister(registers, b))
                .append("\n");
    }

    private static void returnNode(
            StringBuilder builder,
            Map<Node, Register> registers,
            ReturnNode r
    ) {
        builder
                .append("movl ")
                .append(getRegister(registers, predecessorSkipProj(r, ReturnNode.RESULT)))
                .append(", %eax\n")
                .append("ret")
                .append("\n");
    }

    private static Register getRegister(Map<Node, Register> registers, Node node) {
        Register register = registers.get(node);
        if (register == null) {
            throw new IllegalStateException("No register allocated for node: " + node);
        }
        return register;
    }

    private static void signExtendedBinary(
            StringBuilder builder,
            Map<Node, Register> registers,
            BinaryOperationNode node,
            String operation,
            String opcode,
            String outputRegister
    ) {
        var writeTo = getRegister(registers, node);
        Register leftOp = getRegister(registers, predecessorSkipProj(node, BinaryOperationNode.LEFT));
        Register rightOp = getRegister(registers, predecessorSkipProj(node, BinaryOperationNode.RIGHT));
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
