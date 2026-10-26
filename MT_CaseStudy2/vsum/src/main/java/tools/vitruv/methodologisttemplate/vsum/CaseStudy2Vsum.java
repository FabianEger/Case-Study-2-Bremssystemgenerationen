package tools.vitruv.methodologisttemplate.vsum;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;

import cameo_architecture.BlockRepository;
import cameo_architecture.Cameo_architectureFactory;
import cameo_functions.Cameo_functionsFactory;
import cameo_functions.FunctionsRepository;
import cameo_requirements.Cameo_requirementsFactory;
import cameo_requirements.RequirementsRepository;
import mir.reactions.productArchitectureToValidationArchitecture.ProductArchitectureToValidationArchitectureChangePropagationSpecification;
import mir.reactions.productArchitectureToValidationRequirements.ProductArchitectureToValidationRequirementsChangePropagationSpecification;
import mir.reactions.productFunctionsToValidationFunctions.ProductFunctionsToValidationFunctionsChangePropagationSpecification;
import mir.reactions.productFunctionsToValidationRequirements.ProductFunctionsToValidationRequirementsChangePropagationSpecification;
import product_architecture.ProductArchitectureRepository;
import product_architecture.Product_architectureFactory;
import product_functions.ProductFunctionsRepository;
import product_functions.Product_functionsFactory;
import product_requirements.ProductRequirementsRepository;
import product_requirements.Product_requirementsFactory;
import tools.vitruv.change.propagation.ChangePropagationMode;
import tools.vitruv.change.testutils.TestUserInteraction;
import tools.vitruv.framework.views.CommittableView;
import tools.vitruv.framework.views.View;
import tools.vitruv.framework.views.ViewTypeFactory;
import tools.vitruv.framework.vsum.VirtualModel;
import tools.vitruv.framework.vsum.VirtualModelBuilder;
import tools.vitruv.framework.vsum.internal.InternalVirtualModel;

/**
 * Case Study 2 (brake-system generations): set-up of the V-SUM and view helpers.
 *
 * <p>The V-SUM combines two views on the same artefacts (slide 9):
 * <ul>
 *   <li><b>product developer view</b> - one {@code product_requirements}, {@code product_functions}
 *       and {@code product_architecture} model per brake-system generation (Gn-1, Gn, Gn+1);</li>
 *   <li><b>validation engineer view</b> - one Cameo model of the validation environment (VE)
 *       ({@code cameo_requirements}, {@code cameo_functions}, {@code cameo_architecture}) in which the
 *       generations are expressed by generation tags and one validation configuration per generation.</li>
 * </ul>
 *
 * <p>Only the product-to-VE consistency preservation rules of Case Study 2 are registered:
 * CPRs "1" (product functions -&gt; VE requirements), CPRs "2" (product architecture -&gt; VE
 * requirements) and the generation carry-over of the VE functions and configurations. As stated on
 * slide 2, there are deliberately no CPRs between product requirements and VE requirements, between the
 * two architectures, or between the internal elements of one system model; those relations are created
 * by the stakeholders themselves.
 */
public final class CaseStudy2Vsum {

  /** Root types of the product developer view. */
  public static final List<Class<?>> PRODUCT_ROOTS = List.of(
      ProductRequirementsRepository.class, ProductFunctionsRepository.class, ProductArchitectureRepository.class);

  /** Root types of the validation engineer view. */
  public static final List<Class<?>> VALIDATION_ENVIRONMENT_ROOTS = List.of(
      RequirementsRepository.class, FunctionsRepository.class, BlockRepository.class);

  public static final String GN_MINUS_1 = "Gn-1";
  public static final String GN = "Gn";
  public static final String GN_PLUS_1 = "Gn+1";

  private CaseStudy2Vsum() {
  }

  /** Builds and initialises the Case Study 2 V-SUM in the given storage folder. */
  public static InternalVirtualModel createVirtualModel(Path storageFolder) {
    Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().putIfAbsent("*", new XMIResourceFactoryImpl());
    Product_requirementsFactory.eINSTANCE.getClass();
    Product_functionsFactory.eINSTANCE.getClass();
    Product_architectureFactory.eINSTANCE.getClass();
    Cameo_requirementsFactory.eINSTANCE.getClass();
    Cameo_functionsFactory.eINSTANCE.getClass();
    Cameo_architectureFactory.eINSTANCE.getClass();
    InternalVirtualModel model = new VirtualModelBuilder()
        .withStorageFolder(storageFolder)
        .withUserInteractorForResultProvider(new TestUserInteraction.ResultProvider(new TestUserInteraction()))
        .withChangePropagationSpecifications(
            new ProductFunctionsToValidationRequirementsChangePropagationSpecification(),
            new ProductArchitectureToValidationRequirementsChangePropagationSpecification(),
            new ProductFunctionsToValidationFunctionsChangePropagationSpecification(),
            new ProductArchitectureToValidationArchitectureChangePropagationSpecification())
        .buildAndInitialize();
    model.setChangePropagationMode(ChangePropagationMode.TRANSITIVE_CYCLIC);
    return model;
  }

  /** Creates a read view containing all roots of the given types. */
  public static View view(VirtualModel vsum, Collection<Class<?>> rootTypes) {
    var selector = vsum.createSelector(ViewTypeFactory.createIdentityMappingViewType("default"));
    selector.getSelectableElements().stream()
        .filter(element -> rootTypes.stream().anyMatch(type -> type.isInstance(element)))
        .forEach(element -> selector.setSelected(element, true));
    return selector.createView();
  }

  /** Opens a change-recording view on the given root types, applies the modification and commits it. */
  public static void modify(VirtualModel vsum, Collection<Class<?>> rootTypes, Consumer<CommittableView> modification) {
    CommittableView view = view(vsum, rootTypes).withChangeRecordingTrait();
    modification.accept(view);
    view.commitChanges();
  }

  /** File URI of a model inside the V-SUM storage folder. */
  public static URI modelUri(Path storageFolder, String fileName) {
    return URI.createFileURI(storageFolder.toAbsolutePath().resolve(fileName).toString());
  }

  /** File-name friendly representation of a generation ("Gn+1" -&gt; "GnPlus1"). */
  public static String fileSuffix(String generation) {
    return generation.replace("+", "Plus").replace("-", "Minus");
  }

  // ---------------------------------------------------------------------------
  // Root lookup
  // ---------------------------------------------------------------------------

  public static ProductRequirementsRepository productRequirements(View view, String generation) {
    return view.getRootObjects(ProductRequirementsRepository.class).stream()
        .filter(r -> generation.equals(r.getGeneration())).findFirst()
        .orElseThrow(() -> new IllegalStateException("No product requirements model for " + generation));
  }

  public static ProductFunctionsRepository productFunctions(View view, String generation) {
    return view.getRootObjects(ProductFunctionsRepository.class).stream()
        .filter(r -> generation.equals(r.getGeneration())).findFirst()
        .orElseThrow(() -> new IllegalStateException("No product functions model for " + generation));
  }

  public static ProductArchitectureRepository productArchitecture(View view, String generation) {
    return view.getRootObjects(ProductArchitectureRepository.class).stream()
        .filter(r -> generation.equals(r.getGeneration())).findFirst()
        .orElseThrow(() -> new IllegalStateException("No product architecture model for " + generation));
  }

  public static RequirementsRepository veRequirements(View view) {
    return view.getRootObjects(RequirementsRepository.class).iterator().next();
  }

  public static FunctionsRepository veFunctions(View view) {
    return view.getRootObjects(FunctionsRepository.class).iterator().next();
  }

  public static BlockRepository veArchitecture(View view) {
    return view.getRootObjects(BlockRepository.class).iterator().next();
  }

  /** A view with all product and validation-environment roots. */
  public static View fullView(VirtualModel vsum) {
    return view(vsum, List.of(
        ProductRequirementsRepository.class, ProductFunctionsRepository.class, ProductArchitectureRepository.class,
        RequirementsRepository.class, FunctionsRepository.class, BlockRepository.class));
  }
}
