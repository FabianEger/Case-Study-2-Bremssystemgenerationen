package tools.vitruv.methodologisttemplate.vsum;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

import cameo_architecture.BlockRepository;
import cameo_functions.FunctionsRepository;
import cameo_requirements.Requirement;
import cameo_requirements.RequirementsRepository;
import tools.vitruv.framework.views.View;
import tools.vitruv.framework.vsum.VirtualModel;

/**
 * Shared helpers of the Case Study 2 tests: loading the initial V-SUM snapshot of a scenario into a fresh storage
 * folder under {@code target/} (the final state stays there for manual inspection) and read access to the models.
 */
final class CaseStudy2TestSupport {

  private final Path outputDir;
  private final Path stateDir;

  CaseStudy2TestSupport(String scenarioFolder) {
    this.outputDir = Path.of("target", scenarioFolder).toAbsolutePath();
    this.stateDir = outputDir.resolve("vsum");
    try {
      deleteRecursively(outputDir);
      Files.createDirectories(stateDir);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  Path storageFolder() {
    return stateDir;
  }

  /**
   * Loads the snapshot with the given name (see {@link CaseStudy2Snapshots}) as initial state of the scenario. This
   * is the only snapshot a test loads; all scenario steps are then performed in code.
   */
  VirtualModel loadInitialState(String snapshotName) {
    return CaseStudy2Snapshots.load(CaseStudy2Snapshots.DEFAULT_SNAPSHOT_ROOT.resolve(snapshotName), stateDir);
  }

  static RequirementsRepository veRequirements(VirtualModel vsum) {
    return CaseStudy2Vsum.veRequirements(validationView(vsum));
  }

  static FunctionsRepository veFunctions(VirtualModel vsum) {
    return CaseStudy2Vsum.veFunctions(validationView(vsum));
  }

  static BlockRepository veArchitecture(VirtualModel vsum) {
    return CaseStudy2Vsum.veArchitecture(validationView(vsum));
  }

  static View validationView(VirtualModel vsum) {
    return CaseStudy2Vsum.view(vsum, CaseStudy2Vsum.VALIDATION_ENVIRONMENT_ROOTS);
  }

  static View productView(VirtualModel vsum) {
    return CaseStudy2Vsum.view(vsum, CaseStudy2Vsum.PRODUCT_ROOTS);
  }

  static List<Requirement> validationNeeds(VirtualModel vsum) {
    return veRequirements(vsum).getRequirements().stream().filter(ValidationNeedCoverage::isValidationNeed).toList();
  }

  static List<String> names(List<Requirement> requirements) {
    return requirements.stream().map(Requirement::getName).toList();
  }

  private static void deleteRecursively(Path path) throws IOException {
    if (!Files.exists(path)) {
      return;
    }
    Files.walkFileTree(path, new SimpleFileVisitor<>() {
      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        Files.delete(file);
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        Files.delete(dir);
        return FileVisitResult.CONTINUE;
      }
    });
  }
}
