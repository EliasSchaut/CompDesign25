package edu.kit.kastel.vads.compiler.parser;

import edu.kit.kastel.vads.compiler.parser.ast.FunctionTree;
import edu.kit.kastel.vads.compiler.parser.ast.NameTree;
import edu.kit.kastel.vads.compiler.parser.ast.ParameterTree;
import edu.kit.kastel.vads.compiler.parser.ast.ProgramTree;
import edu.kit.kastel.vads.compiler.parser.ast.Tree;
import edu.kit.kastel.vads.compiler.parser.ast.TypeTree;
import edu.kit.kastel.vads.compiler.parser.ast.expression.BooleanTree;
import edu.kit.kastel.vads.compiler.parser.ast.expression.FunctionCallTree;
import edu.kit.kastel.vads.compiler.parser.ast.expression.IdentExpressionTree;
import edu.kit.kastel.vads.compiler.parser.ast.expression.LiteralTree;
import edu.kit.kastel.vads.compiler.parser.ast.expression.operation.BinaryOperationTree;
import edu.kit.kastel.vads.compiler.parser.ast.expression.operation.TernaryOperationTree;
import edu.kit.kastel.vads.compiler.parser.ast.expression.operation.UnaryOperationTree;
import edu.kit.kastel.vads.compiler.parser.ast.lvalue.LValueIdentTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.AssignmentTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.BlockTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.DeclarationTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.StatementTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.control.BreakTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.control.ContinueTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.control.ForTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.control.IfTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.control.ReturnTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.control.WhileTree;

/// This is a utility class to help with debugging the parser.
public class Printer {

    private final Tree ast;
    private final StringBuilder builder = new StringBuilder();
    private boolean requiresIndent;
    private int indentDepth;

    public Printer(Tree ast) {
        this.ast = ast;
    }

    public static String print(Tree ast) {
        Printer printer = new Printer(ast);
        printer.printRoot();
        return printer.builder.toString();
    }

    private void printRoot() {
        printTree(this.ast);
    }

    private void printTree(Tree tree) {
        switch (tree) {
            case BlockTree blockTree -> {
                print("{");
                lineBreak();
                this.indentDepth++;
                for (StatementTree statement : blockTree.statements()) {
                    printTree(statement);
                }
                this.indentDepth--;
                print("}");
                lineBreak();
            }
            case FunctionTree functionTree -> {
                printTree(functionTree.returnType());
                space();
                printTree(functionTree.name());
                print("(");
                for (ParameterTree parameter : functionTree.parameters()) {
                    printTree(parameter);
                }
                print("(");
                space();
                printTree(functionTree.body());
            }
            case NameTree nameTree -> print(nameTree.name().asString());
            case ProgramTree programTree -> {
                for (FunctionTree function : programTree.topLevelTrees()) {
                    printTree(function);
                    lineBreak();
                }
            }
            case TypeTree typeTree -> print(typeTree.type().asString());
            case BinaryOperationTree binaryOp -> {
                print("(");
                printTree(binaryOp.lhs());
                print(")");
                space();
                this.builder.append(binaryOp.operatorType());
                space();
                print("(");
                printTree(binaryOp.rhs());
                print(")");
            }
            case LiteralTree literalTree -> this.builder.append(literalTree.value());
            case UnaryOperationTree unaryOp -> {
                print(unaryOp.operator().asString());
                print("(");
                printTree(unaryOp.operand());
                print(")");
            }
            case AssignmentTree assignmentTree -> {
                printTree(assignmentTree.lValue());
                space();
                this.builder.append(assignmentTree.operator().asString());
                space();
                printTree(assignmentTree.expression());
                semicolon();
            }
            case DeclarationTree declarationTree -> {
                printTree(declarationTree.type());
                space();
                printTree(declarationTree.name());
                if (declarationTree.initializer() != null) {
                    print(" = ");
                    printTree(declarationTree.initializer());
                }
                semicolon();
            }
            case ReturnTree returnTree -> {
                print("return ");
                printTree(returnTree.expression());
                semicolon();
            }
            case LValueIdentTree lValueIdentTree -> printTree(lValueIdentTree.name());
            case IdentExpressionTree identExpressionTree -> printTree(identExpressionTree.name());
            case BreakTree _ -> {
                print("break");
                semicolon();
            }
            case ContinueTree _ -> {
                print("continue");
                semicolon();
            }
            case ForTree forTree -> {
                print(forTree.forKeyword().asString());
                space();
                print("(");
                if (forTree.init() != null) printTree(forTree.init());
                semicolon();
                space();
                printTree(forTree.condition());
                semicolon();
                space();
                if (forTree.update() != null) printTree(forTree.update());
                print(")");
                space();
                printTree(forTree.body());
            }
            case IfTree ifTree -> {
                print(ifTree.ifKeyword().asString());
                space();
                print("(");
                printTree(ifTree.condition());
                print(")");
                space();
                printTree(ifTree.thenBlock());
                if (ifTree.elseBlock() != null) {
                    print(" else ");
                    printTree(ifTree.elseBlock());
                }
            }
            case WhileTree whileTree -> {
                print(whileTree.whileKeyword().asString());
                space();
                print("(");
                printTree(whileTree.condition());
                print(")");
                space();
                printTree(whileTree.body());
            }
            case BooleanTree booleanTree -> print(String.valueOf(booleanTree.value()));
            case TernaryOperationTree ternaryOp -> {
                printTree(ternaryOp.condition());
                print(" ? ");
                printTree(ternaryOp.trueBranch());
                print(" : ");
                printTree(ternaryOp.falseBranch());
            }
            case FunctionCallTree functionCallTree -> {
                printTree(functionCallTree.name());
                print("(");
                boolean first = true;
                for (var arg : functionCallTree.arguments()) {
                    if (!first) {
                        print(", ");
                    } else {
                        first = false;
                    }
                    printTree(arg);
                }
                print(")");
            }
            case ParameterTree parameterTree -> {
                printTree(parameterTree.type());
                space();
                printTree(parameterTree.name());
            }
        }
    }

    private void print(String str) {
        if (this.requiresIndent) {
            this.requiresIndent = false;
            this.builder.append(" ".repeat(4 * this.indentDepth));
        }
        this.builder.append(str);
    }

    private void lineBreak() {
        this.builder.append("\n");
        this.requiresIndent = true;
    }

    private void semicolon() {
        this.builder.append(";");
        lineBreak();
    }

    private void space() {
        this.builder.append(" ");
    }

}
