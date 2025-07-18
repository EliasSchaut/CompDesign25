package edu.kit.kastel.vads.compiler;

import edu.kit.kastel.vads.compiler.backend.aasm.CodeGenerator;
import edu.kit.kastel.vads.compiler.backend.compiler.GccCompiler;
import edu.kit.kastel.vads.compiler.ir.IrGraph;
import edu.kit.kastel.vads.compiler.ir.SsaTranslation;
import edu.kit.kastel.vads.compiler.ir.optimize.LocalValueNumbering;
import edu.kit.kastel.vads.compiler.ir.util.GraphVizPrinter;
import edu.kit.kastel.vads.compiler.ir.util.YCompPrinter;
import edu.kit.kastel.vads.compiler.lexer.Lexer;
import edu.kit.kastel.vads.compiler.parser.ParseException;
import edu.kit.kastel.vads.compiler.parser.Parser;
import edu.kit.kastel.vads.compiler.parser.Printer;
import edu.kit.kastel.vads.compiler.parser.TokenSource;
import edu.kit.kastel.vads.compiler.parser.ast.ProgramTree;
import edu.kit.kastel.vads.compiler.semantic.SemanticException;
import edu.kit.kastel.vads.compiler.semantic.analysis.SemanticAnalysis;
import edu.kit.kastel.vads.compiler.semantic.optimizer.SemanticOptimization;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
    public static void main(String[] args) throws IOException {
        var logger = Logger.getLogger(Main.class.getName());
        if (args.length != 2) {
            logger.severe("Invalid arguments: Expected one input file and one output file");
            System.exit(3);
        }
        Path input = Path.of(args[0]);
        Path output = Path.of(args[1]);
        ProgramTree program = lexAndParse(input);
        try {
            new SemanticAnalysis(program).analyze();
        } catch (SemanticException e) {
            e.printStackTrace();
            System.exit(7);
            return;
        }

        // Print before optimizations
        if (System.getenv("PRINT_PROGRAM") != null || System.getProperty("printProgram") != null) {
            logger.log(Level.INFO, "Program before optimizations:");
            String programCode = Printer.print(program);
            logger.log(Level.INFO, programCode);
        }

        // Apply semantic optimizations
        new SemanticOptimization(program).optimize();

        // Print after optimizations
        if (System.getenv("PRINT_PROGRAM") != null || System.getProperty("printProgram") != null) {
            logger.log(Level.INFO, "Program after optimizations:");
            String programCode = Printer.print(program);
            logger.log(Level.INFO, programCode);
        }

        // SSA translation
        List<IrGraph> graphs = program.topLevelTrees().stream()
                .map(f -> new SsaTranslation(f, new LocalValueNumbering()))
                .map(SsaTranslation::translate)
                .toList();
        dumpIrGraph(graphs, output);

        // Generate code
        String assemblyCode = new CodeGenerator().generateCode(graphs);
        var aasmPath = output.resolveSibling(output.getFileName() + ".s");
        Files.writeString(aasmPath, assemblyCode);

        // Compile to binary
        var compiler = new GccCompiler();
        int exitCode = compiler.compileTo(aasmPath, output);

        if (exitCode != 0) {
            logger.log(Level.SEVERE, "Compilation failed with exit code: {0}", exitCode);
            System.exit(exitCode);
        }
    }

    private static ProgramTree lexAndParse(Path input) throws IOException {
        try {
            Lexer lexer = Lexer.forString(Files.readString(input));
            TokenSource tokenSource = new TokenSource(lexer);
            Parser parser = new Parser(tokenSource);
            return parser.parseProgram();
        } catch (ParseException e) {
            e.printStackTrace();
            System.exit(42);
            throw new AssertionError("unreachable");
        }
    }

    private static void dumpIrGraph(List<IrGraph> graphs, Path output) throws IOException {
        if ("vcg".equals(System.getenv("DUMP_GRAPHS")) || "vcg".equals(System.getProperty("dumpGraphs"))) {
            for (IrGraph graph : graphs) {
                dump("vcg", output, YCompPrinter.print(graph));
            }
        }
        if ("dot".equals(System.getenv("DUMP_GRAPHS")) || "dot".equals(System.getProperty("dumpGraphs"))) {
            for (IrGraph graph : graphs) {
                dump("dot", output, GraphVizPrinter.print(graph));
            }
        }
    }

    private static void dump(String name, Path path, String body) throws IOException {
        Path tmp = path.toAbsolutePath().resolveSibling("graphs");
        if (!Files.exists(tmp)) {
            Files.createDirectory(tmp);
        }
        Files.writeString(
            tmp.resolve(name + "-before-codegen." + name),
            body
        );
    }
}
