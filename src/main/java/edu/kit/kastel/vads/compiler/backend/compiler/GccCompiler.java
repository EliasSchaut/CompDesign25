package edu.kit.kastel.vads.compiler.backend.compiler;

import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Logger;

public class GccCompiler implements Compiler {
  @Override
  public int compileTo(Path assemblyFilePath, Path outputPath) {
    var processBuilder = new ProcessBuilder();
    processBuilder.command(
        "gcc",
        "-z", "noexecstack",
        assemblyFilePath.toString(),
        "-o", outputPath.toString());

    try {
      var process = processBuilder.start();

      var logger = Logger.getLogger(getClass().getName());
      var bufferedReader = process.inputReader();
      bufferedReader.lines().forEach(logger::info);

      bufferedReader = process.errorReader();
      bufferedReader.lines().forEach(logger::severe);

      return process.waitFor();
    } catch (IOException | InterruptedException _) {
      Thread.currentThread().interrupt();
      return -1;
    }
  }
}
