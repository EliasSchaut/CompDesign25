package edu.kit.kastel.vads.compiler.backend.compiler;

import java.nio.file.Path;

interface Compiler {
  int compileTo(Path assemblyFilePath, Path outputPath);
}
