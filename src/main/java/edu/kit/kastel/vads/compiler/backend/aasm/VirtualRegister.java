package edu.kit.kastel.vads.compiler.backend.aasm;

import edu.kit.kastel.vads.compiler.backend.regalloc.Register;

public record VirtualRegister(int id) implements Register {
    @Override
    public String toString() {
        // not using rax, rbx and rsp to avoid conflicts
        return switch (id()) {
            case 0 -> "%rcx";
            case 1 -> "%rbx";
            case 2 -> "%rbp";
            case 3 -> "%rsi";
            case 4 -> "%rdi";
            default -> "%r" + (id() + 3);
        };
    }
}
