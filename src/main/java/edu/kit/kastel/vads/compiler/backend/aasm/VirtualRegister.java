package edu.kit.kastel.vads.compiler.backend.aasm;

import edu.kit.kastel.vads.compiler.backend.regalloc.Register;

public record VirtualRegister(int id) implements Register {
    public static final int MAX_REGISTER_COUNT = 11;
    private static final int MAX_REGISTER_ID = MAX_REGISTER_COUNT - 1;
    private static final String FREE_HAND_REGISTER = "%rcx";

    @Override
    public String toString() {
        return getRegisterString(id());
    }

    public boolean isStackVariable() {
        return id() > MAX_REGISTER_ID;
    }

    public String getFreeHandRegister() {
        return FREE_HAND_REGISTER;
    }

    private String getStackVariableString() {
        var offset = id() - MAX_REGISTER_COUNT;
        return offset + "(%rsp)";
    }

    private String getRegisterString(int id) {
        // not using rax, rdx, rbp and rsp to avoid conflicts
        // rax is used for return values
        // rdx is used for division
        // rbp is used for stack frame pointer
        // rsp is used for stack pointer
        // rcx is our custom free hand for loading and storing values in the stack
        return switch (id) {
            case 0 -> "%rbx";
            case 1 -> "%rsi";
            case 2 -> "%rdi";
            case 3 -> "%r8";
            case 4 -> "%r9";
            case 5 -> "%r10";
            case 6 -> "%r11";
            case 7 -> "%r12";
            case 8 -> "%r13";
            case 9 -> "%r14";
            case 10 -> "%r15";
            default -> getStackVariableString();
        };
    }
}
