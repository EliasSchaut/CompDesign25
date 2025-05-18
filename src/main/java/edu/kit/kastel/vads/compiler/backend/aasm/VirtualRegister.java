package edu.kit.kastel.vads.compiler.backend.aasm;

import edu.kit.kastel.vads.compiler.backend.regalloc.Register;

public record VirtualRegister(int id) implements Register {
    public static final int REGISTER_BYTE_SIZE = 4;
    public static final int MAX_REGISTER_COUNT = 11;
    private static final int MAX_REGISTER_ID = MAX_REGISTER_COUNT - 1;
    private static final String FREE_HAND_REGISTER = "%ecx";

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
        // offset times 4 because we are using 32 bit registers
        return offset * REGISTER_BYTE_SIZE + "(%esp)";
    }

    private String getRegisterString(int id) {
        // not using eax, edx, ebp and esp to avoid conflicts
        // eax is used for return values and for custom intermediate values in operations
        // edx is used for division
        // ebp is used for stack frame pointer
        // esp is used for stack pointer
        // ecx is our custom free hand for loading and storing values in the stack
        return switch (id) {
            case 0 -> "%ebx";
            case 1 -> "%esi";
            case 2 -> "%edi";
            case 3 -> "%r8d";
            case 4 -> "%r9d";
            case 5 -> "%r10d";
            case 6 -> "%r11d";
            case 7 -> "%r12d";
            case 8 -> "%r13d";
            case 9 -> "%r14d";
            case 10 -> "%r15d";
            default -> getStackVariableString();
        };
    }
}
