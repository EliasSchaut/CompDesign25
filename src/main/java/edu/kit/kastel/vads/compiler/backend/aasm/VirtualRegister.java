package edu.kit.kastel.vads.compiler.backend.aasm;

import edu.kit.kastel.vads.compiler.backend.regalloc.Register;

public record VirtualRegister(int id) implements Register {
    @Override
    public String toString() {
        // not using rax, rdx, rbp and rsp to avoid conflicts
        // rax is used for return values
        // rdx is used for division
        // rbp is used for stack frame pointer
        // rsp is used for stack pointer
        // rcx is our custom free hand for loading and storing values in the stack
        return switch (id()) {
            case 0 -> "%rbx";
            case 1 -> "%rsi";
            case 2 -> "%rdi";
            default -> "%r" + (id() + 5);
        };
    }
}
