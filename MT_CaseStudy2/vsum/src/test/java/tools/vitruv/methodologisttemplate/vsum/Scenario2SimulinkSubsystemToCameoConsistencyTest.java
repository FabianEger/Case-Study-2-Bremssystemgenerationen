package tools.vitruv.methodologisttemplate.vsum;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import cameo_architecture.Block;
import cameo_architecture.BlockRepository;
import cameo_architecture.Cameo_architectureFactory;
import cameo_functions.Cameo_functionsFactory;
import cameo_functions.Function;
import cameo_functions.FunctionsRepository;
import cameo_requirements.Cameo_requirementsFactory;
import cameo_requirements.Requirement;
import cameo_requirements.RequirementsRepository;
import mir.reactions.architectureToFunctions.ArchitectureToFunctionsChangePropagationSpecification;
import mir.reactions.architectureToRequirements.ArchitectureToRequirementsChangePropagationSpecification;
import mir.reactions.architectureToSimulink.ArchitectureToSimulinkChangePropagationSpecification;
import mir.reactions.functionsToArchitecture.FunctionsToArchitectureChangePropagationSpecification;
import mir.reactions.functionsToRequirements.FunctionsToRequirementsChangePropagationSpecification;
import mir.reactions.requirementsToArchitecture.RequirementsToArchitectureChangePropagationSpecification;
import mir.reactions.requirementsToFunctions.RequirementsToFunctionsChangePropagationSpecification;
import mir.reactions.simulinkToArchitecture.SimulinkToArchitectureChangePropagationSpecification;
import simulink.SimuLinkFactory;
import simulink.SimulinkModel;
import simulink.SubSystem;
import tools.vitruv.change.propagation.ChangePropagationMode;
import tools.vitruv.change.testutils.TestUserInteraction;
import tools.vitruv.framework.views.CommittableView;
import tools.vitruv.framework.views.View;
import tools.vitruv.framework.views.ViewTypeFactory;
import tools.vitruv.framework.vsum.VirtualModel;
import tools.vitruv.framework.vsum.VirtualModelBuilder;
import tools.vitruv.framework.vsum.internal.InternalVirtualModel;

/**
 * Scenario 2: Simulink SubSystem -> Cameo Architecture / Functions /
 * Requirements consistency.
 *
 * <p>This integration test exercises the inverse direction of Scenario 1.
 * Starting from an empty {@link RequirementsRepository}, the
 * {@link BlockRepository}, {@link FunctionsRepository} and
 * {@link SimulinkModel} are first created via the existing reactions. A
 * {@link SubSystem} is then inserted directly into the {@link SimulinkModel}
 * through the Simulink view. The newly added consistency rules should then:
 * <ol>
 *   <li>create a matching architecture {@link Block},</li>
 *   <li>which in turn creates a matching {@link Requirement},</li>
 *   <li>which in turn creates a matching {@link cameo_functions.Function}.</li>
 * </ol>
 * Finally the {@link Requirement} is renamed and the rename has to propagate
 * to the architecture {@link Block}, the {@link cameo_functions.Function}
 * and the originating {@link SubSystem}.
 */
public class Scenario2SimulinkSubsystemToCameoConsistencyTest {

  private static final String SUBSYSTEM_INITIAL_NAME = "TemperatureMonitoring";
  private static final String REQUIREMENT_RENAMED_NAME = "TemperatureMonitor";

  /**
   * Root output folder for this scenario. Lives under {@code target/} (which
   * is git-ignored and untouched by {@code mvn test}) so that the VSUM state
   * survives the test run and can be inspected manually afterwards.
   */
  private static final Path SCENARIO_OUTPUT_DIR =
      Path.of("target", "scenario2-vsum-output");

  /**
   * Storage folder handed to {@link VirtualModelBuilder#withStorageFolder}.
   * Contains the model files at the top level and the {@code vsum/}
   * subfolder with the correspondences and consistency metadata.
   */
  private static final Path VSUM_STATE_DIR = SCENARIO_OUTPUT_DIR.resolve("vsum");

  /**
   * Folder below which a snapshot of {@link #VSUM_STATE_DIR} is written
   * after each step that triggers a change propagation in the scenario.
   */
  private static final Path VSUM_LOGS_DIR = SCENARIO_OUTPUT_DIR.resolve("logs");

  /** Counter used to prefix snapshot folders with their order of creation. */
  private final AtomicInteger logCounter = new AtomicInteger(0);

  @BeforeAll
  static void setup() {
    Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("*", new XMIResourceFactoryImpl());
  }

  @Test
  void simulinkSubSystemToCameoConsistencyScenario() {
    prepareScenarioOutputDir();
    VirtualModel vsum = createDefaultVirtualModel(VSUM_STATE_DIR);

    // Step 1: register an empty RequirementsRepository as the only root.
    // This triggers the existing reactions to create the FunctionsRepository,
    // BlockRepository and SimulinkModel.
    addRequirementsRepository(vsum, VSUM_STATE_DIR);
    logVsumState("after-repositories-created");
    assertAllRepositoriesPresentAndEmpty(vsum);

    // Step 2: create a SubSystem in the Simulink view.
    addSubSystemToSimulinkModel(vsum, SUBSYSTEM_INITIAL_NAME);
    logVsumState("after-subsystem-created");

    // Step 3: assert that the consistency reactions created a matching
    // architecture Block, cameo_functions Function and cameo_requirements
    // Requirement, all carrying the SubSystem's name.
    assertAllNamesEqual(vsum, SUBSYSTEM_INITIAL_NAME);

    // Step 4: rename the requirement that was created from the SubSystem.
    modifyView(getDefaultView(vsum, List.of(RequirementsRepository.class))
            .withChangeRecordingTrait(),
        (CommittableView v) -> {
          Requirement requirement = v.getRootObjects(RequirementsRepository.class)
              .iterator().next().getRequirements().get(0);
          requirement.setName(REQUIREMENT_RENAMED_NAME);
        });
    logVsumState("after-rename-requirement");

    // Step 5: the rename must have been propagated to Block, Function and
    // SubSystem.
    assertAllNamesEqual(vsum, REQUIREMENT_RENAMED_NAME);
  }

  // VSUM state persistence and logging

  /**
   * Resets {@link #SCENARIO_OUTPUT_DIR} so that the scenario starts from a
   * clean VSUM and an empty log folder, and re-creates the storage folder.
   */
  private void prepareScenarioOutputDir() {
    try {
      deleteRecursivelyIfExists(SCENARIO_OUTPUT_DIR);
      Files.createDirectories(VSUM_STATE_DIR);
      Files.createDirectories(VSUM_LOGS_DIR);
    } catch (IOException e) {
      throw new UncheckedIOException(
          "Could not prepare scenario output directory " + SCENARIO_OUTPUT_DIR.toAbsolutePath(), e);
    }
    logCounter.set(0);
  }

  /**
   * Snapshots the entire VSUM storage folder (model files and the
   * {@code vsum/} subfolder with the correspondences) into
   * {@link #VSUM_LOGS_DIR}. Each snapshot is placed in its own subfolder
   * prefixed with a sequence number so the order of the steps remains
   * visible on disk.
   */
  private void logVsumState(String stepLabel) {
    int index = logCounter.incrementAndGet();
    String folderName = String.format("%02d-%s", index, stepLabel);
    Path target = VSUM_LOGS_DIR.resolve(folderName);
    try {
      copyDirectoryRecursively(VSUM_STATE_DIR, target);
    } catch (IOException e) {
      throw new UncheckedIOException(
          "Could not log VSUM state to " + target.toAbsolutePath(), e);
    }
  }

  private static void copyDirectoryRecursively(Path source, Path target) throws IOException {
    Files.createDirectories(target);
    try (Stream<Path> stream = Files.walk(source)) {
      stream.forEach(sourcePath -> {
        Path destination = target.resolve(source.relativize(sourcePath));
        try {
          if (Files.isDirectory(sourcePath)) {
            Files.createDirectories(destination);
          } else {
            Files.createDirectories(destination.getParent());
            Files.copy(sourcePath, destination, StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.COPY_ATTRIBUTES);
          }
        } catch (IOException e) {
          throw new UncheckedIOException(e);
        }
      });
    }
  }

  private static void deleteRecursivelyIfExists(Path path) throws IOException {
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
        if (exc != null) {
          throw exc;
        }
        Files.delete(dir);
        return FileVisitResult.CONTINUE;
      }
    });
  }

  // ---------------------------------------------------------------------------
  // Custom assertions
  // ---------------------------------------------------------------------------

  /**
   * Asserts that all four repositories exist as roots in the VSUM and are
   * still empty (no requirements / functions / blocks / sub-systems).
   */
  private void assertAllRepositoriesPresentAndEmpty(VirtualModel vsum) {
    View view = getDefaultView(vsum, List.of(
        RequirementsRepository.class,
        FunctionsRepository.class,
        BlockRepository.class,
        SimulinkModel.class));

    Assertions.assertEquals(1, view.getRootObjects(RequirementsRepository.class).size(),
        "expected exactly one RequirementsRepository");
    Assertions.assertEquals(1, view.getRootObjects(FunctionsRepository.class).size(),
        "expected exactly one FunctionsRepository");
    Assertions.assertEquals(1, view.getRootObjects(BlockRepository.class).size(),
        "expected exactly one BlockRepository");
    Assertions.assertEquals(1, view.getRootObjects(SimulinkModel.class).size(),
        "expected exactly one SimulinkModel");

    Assertions.assertTrue(view.getRootObjects(RequirementsRepository.class).iterator().next()
        .getRequirements().isEmpty(), "RequirementsRepository should be empty");
    Assertions.assertTrue(view.getRootObjects(FunctionsRepository.class).iterator().next()
        .getFunctions().isEmpty(), "FunctionsRepository should be empty");
    Assertions.assertTrue(view.getRootObjects(BlockRepository.class).iterator().next()
        .getBlocks().isEmpty(), "BlockRepository should be empty");
    Assertions.assertTrue(view.getRootObjects(SimulinkModel.class).iterator().next()
        .getContains().isEmpty(), "SimulinkModel should be empty");
  }

  /**
   * Asserts that exactly one Requirement, Function, Block and SubSystem
   * exist and that all four carry the given name.
   */
  private void assertAllNamesEqual(VirtualModel vsum, String expectedName) {
    View view = getDefaultView(vsum, List.of(
        RequirementsRepository.class,
        FunctionsRepository.class,
        BlockRepository.class,
        SimulinkModel.class));

    List<Requirement> requirements = view.getRootObjects(RequirementsRepository.class)
        .iterator().next().getRequirements();
    List<Function> functions = view.getRootObjects(FunctionsRepository.class)
        .iterator().next().getFunctions();
    List<Block> blocks = view.getRootObjects(BlockRepository.class)
        .iterator().next().getBlocks();
    List<simulink.Block> subBlocks = view.getRootObjects(SimulinkModel.class)
        .iterator().next().getContains();

    Assertions.assertEquals(1, requirements.size(), "expected exactly one Requirement");
    Assertions.assertEquals(1, functions.size(), "expected exactly one Function");
    Assertions.assertEquals(1, blocks.size(), "expected exactly one architecture Block");
    Assertions.assertEquals(1, subBlocks.size(), "expected exactly one Simulink SubSystem");

    Assertions.assertEquals(expectedName, requirements.get(0).getName(),
        "Requirement should be named " + expectedName);
    Assertions.assertEquals(expectedName, functions.get(0).getName(),
        "Function should be named " + expectedName);
    Assertions.assertEquals(expectedName, blocks.get(0).getName(),
        "architecture Block should be named " + expectedName);
    simulink.Block subBlock = subBlocks.get(0);
    Assertions.assertTrue(subBlock instanceof SubSystem,
        "Simulink container should be a SubSystem");
    Assertions.assertEquals(expectedName, subBlock.getName(),
        "SubSystem should be named " + expectedName);
  }

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

  private void addRequirementsRepository(VirtualModel vsum, Path projectPath) {
    CommittableView view = getDefaultView(vsum, List.of(RequirementsRepository.class)).withChangeRecordingTrait();
    modifyView(view, (CommittableView v) -> {
      v.registerRoot(
          Cameo_requirementsFactory.eINSTANCE.createRequirementsRepository(),
          URI.createFileURI(projectPath.toString() + "/cameo.cameo_requirements"));
    });
  }

  private void addSubSystemToSimulinkModel(VirtualModel vsum, String name) {
    CommittableView view = getDefaultView(vsum, List.of(SimulinkModel.class)).withChangeRecordingTrait();
    modifyView(view, (CommittableView v) -> {
      SubSystem subSystem = SimuLinkFactory.eINSTANCE.createSubSystem();
      subSystem.setName(name);
      v.getRootObjects(SimulinkModel.class).iterator().next().getContains().add(subSystem);
    });
  }

  private InternalVirtualModel createDefaultVirtualModel(Path projectPath) {
    Cameo_requirementsFactory.eINSTANCE.getClass();
    Cameo_functionsFactory.eINSTANCE.getClass();
    Cameo_architectureFactory.eINSTANCE.getClass();
    SimuLinkFactory.eINSTANCE.getClass();
    InternalVirtualModel model = new VirtualModelBuilder()
        .withStorageFolder(projectPath)
        .withUserInteractorForResultProvider(new TestUserInteraction.ResultProvider(new TestUserInteraction()))
        .withChangePropagationSpecifications(
            new ArchitectureToRequirementsChangePropagationSpecification(),
            new ArchitectureToFunctionsChangePropagationSpecification(),
            new ArchitectureToSimulinkChangePropagationSpecification(),
            new FunctionsToRequirementsChangePropagationSpecification(),
            new FunctionsToArchitectureChangePropagationSpecification(),
            new RequirementsToFunctionsChangePropagationSpecification(),
            new RequirementsToArchitectureChangePropagationSpecification(),
            new SimulinkToArchitectureChangePropagationSpecification())
        .buildAndInitialize();
    model.setChangePropagationMode(ChangePropagationMode.TRANSITIVE_CYCLIC);
    return model;
  }

  private View getDefaultView(VirtualModel vsum, Collection<Class<?>> rootTypes) {
    var selector = vsum.createSelector(ViewTypeFactory.createIdentityMappingViewType("default"));
    selector.getSelectableElements().stream()
        .filter(element -> rootTypes.stream().anyMatch(it -> it.isInstance(element)))
        .forEach(it -> selector.setSelected(it, true));
    return selector.createView();
  }

  private void modifyView(CommittableView view, Consumer<CommittableView> modificationFunction) {
    modificationFunction.accept(view);
    view.commitChanges();
  }
}
