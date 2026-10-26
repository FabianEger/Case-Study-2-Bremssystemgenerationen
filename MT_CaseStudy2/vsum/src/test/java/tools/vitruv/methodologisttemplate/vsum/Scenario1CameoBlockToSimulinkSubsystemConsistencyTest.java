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
 * Scenario 1: Cameo Architecture Block -> Simulink SubSystem consistency.
 *
 * <p>This integration test exercises the full chain of Cameo / Simulink
 * consistency reactions through the lens of the {@code cameo_requirements}
 * view:
 * <ol>
 *   <li>A {@link RequirementsRepository} is registered as the only root.</li>
 *   <li>Two {@link Requirement}s are created:
 *       <ul>
 *         <li>{@code TemperatureMonitoring}</li>
 *         <li>{@code Observe the Thermal safety}</li>
 *       </ul>
 *   </li>
 *   <li>For each requirement we assert that a matching
 *       {@link cameo_functions.Function}, {@link Block} and
 *       {@link SubSystem} have been created and carry the same name as the
 *       requirement.</li>
 *   <li>The Simulink {@link SubSystem} that mirrors
 *       {@code TemperatureMonitoring} is then renamed to
 *       {@code MonitorTemperature}, and the test asserts that the rename is
 *       propagated back to the architecture {@link Block}, the Cameo
 *       {@link cameo_functions.Function} and the original
 *       {@link Requirement}.</li>
 * </ol>
 */
public class Scenario1CameoBlockToSimulinkSubsystemConsistencyTest {

  private static final String REQ_TEMP_MONITORING_NAME = "TemperatureMonitoring";
  private static final String REQ_THERMAL_SAFETY_NAME = "Observe the Thermal safety";
  private static final String SUBSYSTEM_RENAMED_NAME = "MonitorTemperature";

  /**
   * Root output folder for this scenario. Lives under {@code target/} (which
   * is git-ignored and untouched by {@code mvn test}) so that the VSUM state
   * survives the test run and can be inspected manually afterwards.
   */
  private static final Path SCENARIO_OUTPUT_DIR =
      Path.of("target", "scenario1-vsum-output");

  /**
   * Storage folder handed to {@link VirtualModelBuilder#withStorageFolder}.
   * Contains the model files at the top level and the {@code vsum/}
   * subfolder with the correspondences and consistency metadata.
   */
  private static final Path VSUM_STATE_DIR = SCENARIO_OUTPUT_DIR.resolve("vsum");

  private static final Path VSUM_LOGS_DIR = SCENARIO_OUTPUT_DIR.resolve("logs");

  /** Counter used to prefix snapshot folders with their order of creation. */
  private final AtomicInteger logCounter = new AtomicInteger(0);

  @BeforeAll
  static void setup() {
    Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("*", new XMIResourceFactoryImpl());
  }

  @Test
  void cameoBlockToSimulinkSubsystemConsistencyScenario() {
    prepareScenarioOutputDir();
    VirtualModel vsum = createDefaultVirtualModel(VSUM_STATE_DIR);

    // Step 1: check out the cameo_requirements view by registering an empty
    // RequirementsRepository as root. This triggers creation of the
    // FunctionsRepository, BlockRepository and SimulinkModel via the existing
    // reactions.
    addRequirementsRepository(vsum, VSUM_STATE_DIR);

    // Step 2: create the two requirements through the requirements view only.
    addRequirement(vsum, "REQ-1", REQ_TEMP_MONITORING_NAME);
    logVsumState("after-requirement-temperature-monitoring");
    addRequirement(vsum, "REQ-2", REQ_THERMAL_SAFETY_NAME);
    logVsumState("after-requirement-thermal-safety");

    // Step 3: assert that for each requirement a matching Function, Block and
    // SubSystem exists and that the names are propagated correctly.
    assertAllNamesEqual(vsum, REQ_TEMP_MONITORING_NAME, REQ_THERMAL_SAFETY_NAME);

    // Step 4: rename the SubSystem that mirrors TemperatureMonitoring to
    // MonitorTemperature in the Simulink view.
    modifyView(getDefaultView(vsum, List.of(SimulinkModel.class)).withChangeRecordingTrait(),
        (CommittableView v) -> {
          SubSystem tempMonitoringSubSystem = findSubSystemByName(
              v.getRootObjects(SimulinkModel.class).iterator().next(),
              REQ_TEMP_MONITORING_NAME);
          tempMonitoringSubSystem.setName(SUBSYSTEM_RENAMED_NAME);
        });

    logVsumState("after-rename-subsystem");

    // Step 5: the rename must have been propagated all the way back to the
    // requirement, while the second requirement remains untouched.
    assertAllNamesEqual(vsum, SUBSYSTEM_RENAMED_NAME, REQ_THERMAL_SAFETY_NAME);
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
   * Asserts that exactly two requirements with the given names exist and that
   * each one has a matching Function, Block and SubSystem (same name, same
   * order).
   */
  private void assertAllNamesEqual(VirtualModel vsum, String firstName, String secondName) {
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

    Assertions.assertEquals(2, requirements.size(), "expected two requirements");
    Assertions.assertEquals(2, functions.size(), "expected two functions");
    Assertions.assertEquals(2, blocks.size(), "expected two architecture blocks");
    Assertions.assertEquals(2, subBlocks.size(), "expected two simulink sub-systems");

    assertNameAt(requirements, functions, blocks, subBlocks, 0, firstName);
    assertNameAt(requirements, functions, blocks, subBlocks, 1, secondName);
  }

  private void assertNameAt(List<Requirement> requirements, List<Function> functions,
      List<Block> blocks, List<simulink.Block> subBlocks, int index, String expectedName) {
    Assertions.assertEquals(expectedName, requirements.get(index).getName(),
        "Requirement[" + index + "] should be named " + expectedName);
    Assertions.assertEquals(expectedName, functions.get(index).getName(),
        "Function[" + index + "] should be named " + expectedName);
    Assertions.assertEquals(expectedName, blocks.get(index).getName(),
        "architecture Block[" + index + "] should be named " + expectedName);
    simulink.Block subBlock = subBlocks.get(index);
    Assertions.assertTrue(subBlock instanceof SubSystem,
        "Simulink container at " + index + " should be a SubSystem");
    Assertions.assertEquals(expectedName, subBlock.getName(),
        "SubSystem[" + index + "] should be named " + expectedName);
  }

  private SubSystem findSubSystemByName(SimulinkModel simulinkModel, String name) {
    return simulinkModel.getContains().stream()
        .filter(SubSystem.class::isInstance)
        .map(SubSystem.class::cast)
        .filter(s -> name.equals(s.getName()))
        .findFirst()
        .orElseThrow(() -> new AssertionError("No SubSystem named '" + name + "' found"));
  }

  // ---------------------------------------------------------------------------
  // Helpers (mirrors the helpers used in the other VSUM tests)
  // ---------------------------------------------------------------------------

  private void addRequirementsRepository(VirtualModel vsum, Path projectPath) {
    CommittableView view = getDefaultView(vsum, List.of(RequirementsRepository.class)).withChangeRecordingTrait();
    modifyView(view, (CommittableView v) -> {
      v.registerRoot(
          Cameo_requirementsFactory.eINSTANCE.createRequirementsRepository(),
          URI.createFileURI(projectPath.toString() + "/cameo.cameo_requirements"));
    });
  }

  private void addRequirement(VirtualModel vsum, String id, String name) {
    CommittableView view = getDefaultView(vsum, List.of(RequirementsRepository.class)).withChangeRecordingTrait();
    modifyView(view, (CommittableView v) -> {
      Requirement requirement = Cameo_requirementsFactory.eINSTANCE.createRequirement();
      requirement.setId(id);
      requirement.setName(name);
      requirement.setText(name);
      v.getRootObjects(RequirementsRepository.class).iterator().next().getRequirements().add(requirement);
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
