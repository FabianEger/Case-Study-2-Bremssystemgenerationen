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
import cameo_functions.FunctionsRepository;
import cameo_requirements.Cameo_requirementsFactory;
import cameo_requirements.Requirement;
import cameo_requirements.RequirementsRepository;
import mir.reactions.architectureToRequirements.ArchitectureToRequirementsChangePropagationSpecification;
import mir.reactions.architectureToFunctions.ArchitectureToFunctionsChangePropagationSpecification;
import mir.reactions.functionsToArchitecture.FunctionsToArchitectureChangePropagationSpecification;
import mir.reactions.functionsToRequirements.FunctionsToRequirementsChangePropagationSpecification;
import mir.reactions.requirementsToFunctions.RequirementsToFunctionsChangePropagationSpecification;
import mir.reactions.requirementsToArchitecture.RequirementsToArchitectureChangePropagationSpecification;
import tools.vitruv.change.propagation.ChangePropagationMode;
import tools.vitruv.change.testutils.TestUserInteraction;
import tools.vitruv.framework.views.CommittableView;
import tools.vitruv.framework.views.View;
import tools.vitruv.framework.views.ViewTypeFactory;
import tools.vitruv.framework.vsum.VirtualModel;
import tools.vitruv.framework.vsum.VirtualModelBuilder;
import tools.vitruv.framework.vsum.internal.InternalVirtualModel;

/**
 * Tests for the consistency reactions between the three Cameo metamodels:
 * cameo_requirements, cameo_functions and cameo_architecture.
 *
 * <p>The reactions under test (see
 * {@code consistency/src/main/reactions/.../cameoReactions.reactions}) keep
 * a {@link Requirement}, its corresponding {@link cameo_functions.Function}
 * and its corresponding {@link cameo_architecture.Block} in sync:
 * <ul>
 *   <li>creating a Requirement creates a Function and a Block (with the same name)
 *       and registers a correspondence between the three;</li>
 *   <li>renaming any of the three updates the other two;</li>
 *   <li>removing the Requirement removes the Function and the Block.</li>
 * </ul>
 */
public class VSUMExampleTest {

  @BeforeAll
  static void setup() {
    Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("*", new XMIResourceFactoryImpl());
  }



  @Test
  void reloadEmptyVirtualModel(@TempDir Path tempDir) {
    InternalVirtualModel vsum = createDefaultVirtualModel(tempDir);
    vsum.dispose();
    vsum = createDefaultVirtualModel(tempDir);
  }

  @Test
  void reloadFilledVirtualModel(@TempDir Path tempDir) {
    InternalVirtualModel vsum = createDefaultVirtualModel(tempDir);
    addRequirementsRepository(vsum, tempDir);
    vsum.dispose();
    vsum = createDefaultVirtualModel(tempDir);
    Assertions.assertEquals(1,
        getDefaultView(vsum, List.of(RequirementsRepository.class)).getRootObjects().size());
    Assertions.assertEquals(1,
        getDefaultView(vsum, List.of(FunctionsRepository.class)).getRootObjects().size());
    Assertions.assertEquals(1,
        getDefaultView(vsum, List.of(BlockRepository.class)).getRootObjects().size());
  }

  @Test
  void requirementsRepositoryInsertionCreatesCompanionRepositories(@TempDir Path tempDir) {
    VirtualModel vsum = createDefaultVirtualModel(tempDir);
    addRequirementsRepository(vsum, tempDir);

    Assertions.assertEquals(1,
        getDefaultView(vsum, List.of(RequirementsRepository.class)).getRootObjects().size());
    Assertions.assertEquals(1,
        getDefaultView(vsum, List.of(FunctionsRepository.class)).getRootObjects().size());
    Assertions.assertEquals(1,
        getDefaultView(vsum, List.of(BlockRepository.class)).getRootObjects().size());
  }

  @Test
  void insertingRequirementCreatesFunctionAndBlock(@TempDir Path tempDir) {
    VirtualModel vsum = createDefaultVirtualModel(tempDir);
    addRequirementsRepository(vsum, tempDir);
    addRequirement(vsum, "REQ-1", "Brake within 5 seconds");

    View view = getDefaultView(vsum,
        List.of(RequirementsRepository.class, FunctionsRepository.class, BlockRepository.class));

    var requirements = view.getRootObjects(RequirementsRepository.class).iterator().next().getRequirements();
    var funs = view.getRootObjects(FunctionsRepository.class).iterator().next().getFunctions();
    var blocks = view.getRootObjects(BlockRepository.class).iterator().next().getBlocks();

    Assertions.assertEquals(1, requirements.size());
    Assertions.assertEquals(1, funs.size());
    Assertions.assertEquals(1, blocks.size());

    String reqName = requirements.get(0).getName();
    Assertions.assertEquals(reqName, funs.get(0).getName());
    Assertions.assertEquals(reqName, blocks.get(0).getName());
  }

  @Test
  void insertingMultipleRequirementsCreatesMatchingPairs(@TempDir Path tempDir) {
    VirtualModel vsum = createDefaultVirtualModel(tempDir);
    addRequirementsRepository(vsum, tempDir);
    addRequirement(vsum, "REQ-1", "First");
    addRequirement(vsum, "REQ-2", "Second");
    addRequirement(vsum, "REQ-3", "Third");

    View view = getDefaultView(vsum,
        List.of(RequirementsRepository.class, FunctionsRepository.class, BlockRepository.class));

    Assertions.assertEquals(3,
        view.getRootObjects(RequirementsRepository.class).iterator().next().getRequirements().size());
    Assertions.assertEquals(3,
        view.getRootObjects(FunctionsRepository.class).iterator().next().getFunctions().size());
    Assertions.assertEquals(3,
        view.getRootObjects(BlockRepository.class).iterator().next().getBlocks().size());
  }

  @Test
  void renamingRequirementRenamesFunctionAndBlock(@TempDir Path tempDir) {
    final String newName = "Renamed via Requirement";
    VirtualModel vsum = createDefaultVirtualModel(tempDir);
    addRequirementsRepository(vsum, tempDir);
    addRequirement(vsum, "REQ-1", "initial");

    modifyView(getDefaultView(vsum, List.of(RequirementsRepository.class)).withChangeRecordingTrait(),
        (CommittableView v) -> {
          v.getRootObjects(RequirementsRepository.class).iterator().next().getRequirements().get(0)
              .setName(newName);
        });

    View view = getDefaultView(vsum,
        List.of(RequirementsRepository.class, FunctionsRepository.class, BlockRepository.class));
    Assertions.assertEquals(newName,
        view.getRootObjects(RequirementsRepository.class).iterator().next().getRequirements().get(0).getName());
    Assertions.assertEquals(newName,
        view.getRootObjects(FunctionsRepository.class).iterator().next().getFunctions().get(0).getName());
    Assertions.assertEquals(newName,
        view.getRootObjects(BlockRepository.class).iterator().next().getBlocks().get(0).getName());
  }

  @Test
  void renamingFunctionRenamesRequirementAndBlock(@TempDir Path tempDir) {
    final String newName = "Renamed via Function";
    VirtualModel vsum = createDefaultVirtualModel(tempDir);
    addRequirementsRepository(vsum, tempDir);
    addRequirement(vsum, "REQ-1", "initial");

    modifyView(getDefaultView(vsum, List.of(FunctionsRepository.class)).withChangeRecordingTrait(),
        (CommittableView v) -> {
          v.getRootObjects(FunctionsRepository.class).iterator().next().getFunctions().get(0).setName(newName);
        });

    View view = getDefaultView(vsum,
        List.of(RequirementsRepository.class, FunctionsRepository.class, BlockRepository.class));
    Assertions.assertEquals(newName,
        view.getRootObjects(RequirementsRepository.class).iterator().next().getRequirements().get(0).getName());
    Assertions.assertEquals(newName,
        view.getRootObjects(FunctionsRepository.class).iterator().next().getFunctions().get(0).getName());
    Assertions.assertEquals(newName,
        view.getRootObjects(BlockRepository.class).iterator().next().getBlocks().get(0).getName());
  }

  @Test
  void renamingBlockRenamesRequirementAndFunction(@TempDir Path tempDir) {
    final String newName = "Renamed via Block";
    VirtualModel vsum = createDefaultVirtualModel(tempDir);
    addRequirementsRepository(vsum, tempDir);
    addRequirement(vsum, "REQ-1", "initial");

    modifyView(getDefaultView(vsum, List.of(BlockRepository.class))
            .withChangeRecordingTrait(),
        (CommittableView v) -> {
          Block block = v.getRootObjects(BlockRepository.class).iterator().next().getBlocks().get(0);
          System.out.println("Renaming block: " + block.getName());
          block.setName(newName);
          System.out.println("Block name: " + block.getName());
        });

    View view = getDefaultView(vsum,
        List.of(RequirementsRepository.class, FunctionsRepository.class, BlockRepository.class));
    Assertions.assertEquals(newName,
      view.getRootObjects(BlockRepository.class).iterator().next().getBlocks().get(0).getName());
    Assertions.assertEquals(newName,
        view.getRootObjects(RequirementsRepository.class).iterator().next().getRequirements().get(0).getName());
    Assertions.assertEquals(newName,
        view.getRootObjects(FunctionsRepository.class).iterator().next().getFunctions().get(0).getName());
  }

  // ---------------------------------------------------------------------------
  // Deletion
  // ---------------------------------------------------------------------------

  @Test
  void deletingRequirementRemovesFunctionAndBlock(@TempDir Path tempDir) {
    VirtualModel vsum = createDefaultVirtualModel(tempDir);
    addRequirementsRepository(vsum, tempDir);
    addRequirement(vsum, "REQ-1", "to be removed");
    addRequirement(vsum, "REQ-2", "to remain");

    modifyView(getDefaultView(vsum, List.of(RequirementsRepository.class)).withChangeRecordingTrait(),
        (CommittableView v) -> {
          v.getRootObjects(RequirementsRepository.class).iterator().next().getRequirements().remove(0);
        });

    View view = getDefaultView(vsum,
        List.of(RequirementsRepository.class, FunctionsRepository.class, BlockRepository.class));

    var requirements = view.getRootObjects(RequirementsRepository.class).iterator().next().getRequirements();
    var funs = view.getRootObjects(FunctionsRepository.class).iterator().next().getFunctions();
    var blocks = view.getRootObjects(BlockRepository.class).iterator().next().getBlocks();

    Assertions.assertEquals(1, requirements.size());
    Assertions.assertEquals(1, funs.size());
    Assertions.assertEquals(1, blocks.size());
    Assertions.assertEquals("REQ-2", requirements.get(0).getId());
    Assertions.assertEquals(requirements.get(0).getName(), funs.get(0).getName());
    Assertions.assertEquals(requirements.get(0).getName(), blocks.get(0).getName());
  }

  /**
   * Deletion is propagated only Requirement -> Function/Block, not the other way
   * round. Removing a Function from the FunctionsRepository must therefore keep
   * the corresponding Requirement (and the Block) untouched.
   */
  @Test
  void deletingFunctionDoesNotDeleteRequirementOrBlock(@TempDir Path tempDir) {
    VirtualModel vsum = createDefaultVirtualModel(tempDir);
    addRequirementsRepository(vsum, tempDir);
    addRequirement(vsum, "REQ-1", "initial");

    modifyView(getDefaultView(vsum, List.of(FunctionsRepository.class)).withChangeRecordingTrait(),
        (CommittableView v) -> {
          v.getRootObjects(FunctionsRepository.class).iterator().next().getFunctions().remove(0);
        });

    View view = getDefaultView(vsum,
        List.of(RequirementsRepository.class, FunctionsRepository.class, BlockRepository.class));
    Assertions.assertEquals(1,
        view.getRootObjects(RequirementsRepository.class).iterator().next().getRequirements().size());
    Assertions.assertEquals(1,
        view.getRootObjects(BlockRepository.class).iterator().next().getBlocks().size());
    Assertions.assertEquals(0,
        view.getRootObjects(FunctionsRepository.class).iterator().next().getFunctions().size());
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
    // ensure all three Cameo packages are initialized in standalone mode before
    // the VSUM is built.
    Cameo_requirementsFactory.eINSTANCE.getClass();
    Cameo_functionsFactory.eINSTANCE.getClass();
    Cameo_architectureFactory.eINSTANCE.getClass();
    InternalVirtualModel model = new VirtualModelBuilder()
        .withStorageFolder(projectPath)
        .withUserInteractorForResultProvider(new TestUserInteraction.ResultProvider(new TestUserInteraction()))
        .withChangePropagationSpecifications(
          new ArchitectureToRequirementsChangePropagationSpecification(),
          new ArchitectureToFunctionsChangePropagationSpecification(),
          new FunctionsToRequirementsChangePropagationSpecification(),
          new FunctionsToArchitectureChangePropagationSpecification(),
          new RequirementsToFunctionsChangePropagationSpecification(),
          new RequirementsToArchitectureChangePropagationSpecification())
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
