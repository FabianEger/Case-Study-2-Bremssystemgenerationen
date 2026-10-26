package tools.vitruv.methodologisttemplate.vsum;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import cameo_architecture.Block;
import cameo_architecture.BlockRepository;
import cameo_architecture.Cameo_architectureFactory;
import cameo_functions.Cameo_functionsFactory;
import cameo_requirements.Cameo_requirementsFactory;
import cameo_requirements.Requirement;
import cameo_requirements.RequirementsRepository;
import simulink.SimuLinkFactory;
import simulink.SimulinkModel;
import simulink.SubSystem;

import mir.reactions.architectureToFunctions.ArchitectureToFunctionsChangePropagationSpecification;
import mir.reactions.architectureToRequirements.ArchitectureToRequirementsChangePropagationSpecification;
import mir.reactions.architectureToSimulink.ArchitectureToSimulinkChangePropagationSpecification;
import mir.reactions.functionsToArchitecture.FunctionsToArchitectureChangePropagationSpecification;
import mir.reactions.functionsToRequirements.FunctionsToRequirementsChangePropagationSpecification;
import mir.reactions.requirementsToArchitecture.RequirementsToArchitectureChangePropagationSpecification;
import mir.reactions.requirementsToFunctions.RequirementsToFunctionsChangePropagationSpecification;
import mir.reactions.simulinkToArchitecture.SimulinkToArchitectureChangePropagationSpecification;

import tools.vitruv.change.propagation.ChangePropagationMode;
import tools.vitruv.change.testutils.TestUserInteraction;
import tools.vitruv.framework.views.CommittableView;
import tools.vitruv.framework.views.View;
import tools.vitruv.framework.views.ViewTypeFactory;
import tools.vitruv.framework.vsum.VirtualModel;
import tools.vitruv.framework.vsum.VirtualModelBuilder;
import tools.vitruv.framework.vsum.internal.InternalVirtualModel;

/**
 * Tests for the consistency reactions that keep a {@code cameo_architecture}
 * model in sync with a {@code simulink} model.
 *
 * <p>The reactions under test
 * (see {@code cameoArchitectureToSimulink.reactions} and
 * {@code cameoSimulinkToArchitecture.reactions}) implement the following
 * rules:
 * <ul>
 *   <li>Inserting a {@link BlockRepository} as root creates a corresponding
 *       {@link SimulinkModel}.</li>
 *   <li>Inserting a {@link Block} into the BlockRepository creates a
 *       corresponding {@link SubSystem} in the SimulinkModel with the same
 *       name.</li>
 *   <li>Removing a Block from the BlockRepository removes the corresponding
 *       SubSystem.</li>
 *   <li>Renaming either the Block or the SubSystem propagates the new name
 *       to its counterpart.</li>
 * </ul>
 */
public class VSUMSimulinkArchitectureTest {

  @BeforeAll
  static void setup() {
    Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("*", new XMIResourceFactoryImpl());
  }

  // ---------------------------------------------------------------------------
  // Creation
  // ---------------------------------------------------------------------------

  @Test
  void blockRepositoryInsertionCreatesSimulinkModel(@TempDir Path tempDir) {
    VirtualModel vsum = createDefaultVirtualModel(tempDir);
    addBlockRepository(vsum, tempDir);

    Assertions.assertEquals(1,
        getDefaultView(vsum, List.of(BlockRepository.class)).getRootObjects().size());
    Assertions.assertEquals(1,
        getDefaultView(vsum, List.of(SimulinkModel.class)).getRootObjects().size());
  }

  @Test
  void insertingBlockCreatesSubSystemWithSameName(@TempDir Path tempDir) {
    VirtualModel vsum = createDefaultVirtualModel(tempDir);
    addBlockRepository(vsum, tempDir);
    addArchitectureBlock(vsum, "BrakePedal");

    View view = getDefaultView(vsum, List.of(BlockRepository.class, SimulinkModel.class));
    var blocks = view.getRootObjects(BlockRepository.class).iterator().next().getBlocks();
    var subBlocks = view.getRootObjects(SimulinkModel.class).iterator().next().getContains();

    Assertions.assertEquals(1, blocks.size());
    Assertions.assertEquals(1, subBlocks.size());
    Assertions.assertTrue(subBlocks.get(0) instanceof SubSystem,
        "Block created in cameo_architecture should be mirrored as a Simulink SubSystem");
    Assertions.assertEquals("BrakePedal", blocks.get(0).getName());
    Assertions.assertEquals("BrakePedal", subBlocks.get(0).getName());
  }

  @Test
  void insertingMultipleBlocksCreatesMatchingSubSystems(@TempDir Path tempDir) {
    VirtualModel vsum = createDefaultVirtualModel(tempDir);
    addBlockRepository(vsum, tempDir);
    addArchitectureBlock(vsum, "B1");
    addArchitectureBlock(vsum, "B2");
    addArchitectureBlock(vsum, "B3");

    View view = getDefaultView(vsum, List.of(BlockRepository.class, SimulinkModel.class));
    Assertions.assertEquals(3,
        view.getRootObjects(BlockRepository.class).iterator().next().getBlocks().size());
    Assertions.assertEquals(3,
        view.getRootObjects(SimulinkModel.class).iterator().next().getContains().size());
  }

  // ---------------------------------------------------------------------------
  // Renaming (bi-directional)
  // ---------------------------------------------------------------------------

  @Test
  void renamingArchitectureBlockRenamesSubSystem(@TempDir Path tempDir) {
    final String newName = "RenamedFromBlock";
    VirtualModel vsum = createDefaultVirtualModel(tempDir);
    addBlockRepository(vsum, tempDir);
    addArchitectureBlock(vsum, "Initial");

    modifyView(getDefaultView(vsum, List.of(BlockRepository.class)).withChangeRecordingTrait(),
        (CommittableView v) -> {
          v.getRootObjects(BlockRepository.class).iterator().next().getBlocks().get(0)
              .setName(newName);
        });

    View view = getDefaultView(vsum, List.of(BlockRepository.class, SimulinkModel.class));
    Assertions.assertEquals(newName,
        view.getRootObjects(BlockRepository.class).iterator().next().getBlocks().get(0).getName());
    Assertions.assertEquals(newName,
        view.getRootObjects(SimulinkModel.class).iterator().next().getContains().get(0).getName());
  }

  @Test
  void renamingSubSystemRenamesArchitectureBlock(@TempDir Path tempDir) {
    final String newName = "RenamedFromSubSystem";
    VirtualModel vsum = createDefaultVirtualModel(tempDir);
    addBlockRepository(vsum, tempDir);
    addArchitectureBlock(vsum, "Initial");

    modifyView(getDefaultView(vsum, List.of(SimulinkModel.class)).withChangeRecordingTrait(),
        (CommittableView v) -> {
          v.getRootObjects(SimulinkModel.class).iterator().next().getContains().get(0)
              .setName(newName);
        });

    View view = getDefaultView(vsum, List.of(BlockRepository.class, SimulinkModel.class));
    Assertions.assertEquals(newName,
        view.getRootObjects(SimulinkModel.class).iterator().next().getContains().get(0).getName());
    Assertions.assertEquals(newName,
        view.getRootObjects(BlockRepository.class).iterator().next().getBlocks().get(0).getName());
  }

  // ---------------------------------------------------------------------------
  // Deletion
  // ---------------------------------------------------------------------------

  @Test
  void deletingArchitectureBlockRemovesSubSystem(@TempDir Path tempDir) {
    VirtualModel vsum = createDefaultVirtualModel(tempDir);
    addBlockRepository(vsum, tempDir);
    addArchitectureBlock(vsum, "ToBeRemoved");
    addArchitectureBlock(vsum, "ToRemain");

    modifyView(getDefaultView(vsum, List.of(BlockRepository.class)).withChangeRecordingTrait(),
        (CommittableView v) -> {
          v.getRootObjects(BlockRepository.class).iterator().next().getBlocks().remove(0);
        });

    View view = getDefaultView(vsum, List.of(BlockRepository.class, SimulinkModel.class));
    var blocks = view.getRootObjects(BlockRepository.class).iterator().next().getBlocks();
    var subBlocks = view.getRootObjects(SimulinkModel.class).iterator().next().getContains();
    Assertions.assertEquals(1, blocks.size());
    Assertions.assertEquals(1, subBlocks.size());
    Assertions.assertEquals("ToRemain", blocks.get(0).getName());
    Assertions.assertEquals("ToRemain", subBlocks.get(0).getName());
  }

  // ---------------------------------------------------------------------------
  // End-to-end via Requirement (existing reactions cascade)
  // ---------------------------------------------------------------------------

  /**
   * The requirements reactions create a Block automatically when a Requirement
   * is inserted; this exercises the full chain Requirement -> Block ->
   * SubSystem.
   */
  @Test
  void requirementInsertionAlsoCreatesSubSystem(@TempDir Path tempDir) {
    VirtualModel vsum = createDefaultVirtualModel(tempDir);
    addRequirementsRepository(vsum, tempDir);
    addRequirement(vsum, "REQ-1", "BrakeWithin5s");

    View view = getDefaultView(vsum, List.of(BlockRepository.class, SimulinkModel.class));
    Assertions.assertEquals(1,
        view.getRootObjects(BlockRepository.class).iterator().next().getBlocks().size());
    Assertions.assertEquals(1,
        view.getRootObjects(SimulinkModel.class).iterator().next().getContains().size());
    Assertions.assertEquals("BrakeWithin5s",
        view.getRootObjects(SimulinkModel.class).iterator().next().getContains().get(0).getName());
  }

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

  private void addBlockRepository(VirtualModel vsum, Path projectPath) {
    CommittableView view = getDefaultView(vsum, List.of(BlockRepository.class)).withChangeRecordingTrait();
    modifyView(view, (CommittableView v) -> {
      v.registerRoot(
          Cameo_architectureFactory.eINSTANCE.createBlockRepository(),
          URI.createFileURI(projectPath.toString() + "/cameo.cameo_architecture"));
    });
  }

  private void addArchitectureBlock(VirtualModel vsum, String name) {
    CommittableView view = getDefaultView(vsum, List.of(BlockRepository.class)).withChangeRecordingTrait();
    modifyView(view, (CommittableView v) -> {
      Block block = Cameo_architectureFactory.eINSTANCE.createBlock();
      block.setName(name);
      v.getRootObjects(BlockRepository.class).iterator().next().getBlocks().add(block);
    });
  }

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
    // Initialize all four Cameo/Simulink packages in standalone mode before the
    // VSUM is built so EMF can resolve their EPackages by nsURI.
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
