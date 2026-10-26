package tools.vitruv.methodologisttemplate.vsum;

import static tools.vitruv.methodologisttemplate.vsum.CaseStudy2Vsum.GN;
import static tools.vitruv.methodologisttemplate.vsum.CaseStudy2Vsum.GN_MINUS_1;
import static tools.vitruv.methodologisttemplate.vsum.CaseStudy2Vsum.GN_PLUS_1;
import static tools.vitruv.methodologisttemplate.vsum.CaseStudy2Vsum.PRODUCT_ROOTS;
import static tools.vitruv.methodologisttemplate.vsum.CaseStudy2Vsum.VALIDATION_ENVIRONMENT_ROOTS;
import static tools.vitruv.methodologisttemplate.vsum.CaseStudy2Vsum.modify;

import java.nio.file.Path;
import java.util.function.Consumer;

import tools.vitruv.framework.views.CommittableView;
import tools.vitruv.framework.vsum.VirtualModel;

/**
 * Case Study 2: the stakeholder steps of slides 3 and 6 as V-SUM modifications. Every method is one commit
 * through a view of the respective stakeholder; Vitruvius steps (6, 8, 10, 12, 14 and 9 of scenario 2) happen
 * implicitly on commit through the registered CPRs.
 */
public final class CaseStudy2Scenarios {

  private CaseStudy2Scenarios() {
  }

  private static void productDeveloper(VirtualModel vsum, Consumer<CommittableView> change) {
    modify(vsum, PRODUCT_ROOTS, change);
  }

  private static void validationEngineer(VirtualModel vsum, Consumer<CommittableView> change) {
    modify(vsum, VALIDATION_ENVIRONMENT_ROOTS, change);
  }

  /**
   * Initial V-SUM state: product generation Gn-1 (without ABS) and the validation environment for Gn-1.
   * The VE is registered first; the baseline product model then derives no validation needs.
   */
  public static void initialiseGnMinus1(VirtualModel vsum, Path storageFolder) {
    validationEngineer(vsum, v -> CaseStudy2ValidationEnvironmentModels.registerValidationEnvironment(v, storageFolder));
    validationEngineer(vsum, CaseStudy2ValidationEnvironmentModels::populateVnMinus1);
    productDeveloper(vsum, v -> CaseStudy2ProductModels.registerGeneration(v, storageFolder, GN_MINUS_1, null));
    productDeveloper(vsum, v -> CaseStudy2ProductModels.populateGnMinus1(v, GN_MINUS_1));
  }

  /** Steps 1-2 (scenario 1) / 6-7 (scenario 2): new generation based on its predecessor. */
  public static void deriveGeneration(VirtualModel vsum, Path storageFolder, String generation, String predecessor) {
    productDeveloper(vsum, v -> CaseStudy2ProductModels.registerGeneration(v, storageFolder, generation, predecessor));
    productDeveloper(vsum, v -> CaseStudy2ProductModels.copyFromPredecessor(v, generation));
  }

  // --- Scenario 1: Gn-1 -> Gn (ABS)

  public static void gnStep03(VirtualModel vsum) {
    productDeveloper(vsum, CaseStudy2ProductModels::gnStep03AddAntiLockRequirement);
  }

  public static void gnStep04(VirtualModel vsum) {
    productDeveloper(vsum, CaseStudy2ProductModels::gnStep04AddWheelSpeedRequirement);
  }

  public static void gnStep05(VirtualModel vsum) {
    productDeveloper(vsum, CaseStudy2ProductModels::gnStep05AddAbsFunction);
  }

  public static void gnStep07(VirtualModel vsum) {
    productDeveloper(vsum, CaseStudy2ProductModels::gnStep07AddWheelSpeedFunction);
  }

  public static void gnStep09(VirtualModel vsum) {
    productDeveloper(vsum, CaseStudy2ProductModels::gnStep09AddSensors);
  }

  public static void gnStep11(VirtualModel vsum) {
    productDeveloper(vsum, CaseStudy2ProductModels::gnStep11AddAbsControlUnit);
  }

  public static void gnStep13(VirtualModel vsum) {
    productDeveloper(vsum, CaseStudy2ProductModels::gnStep13AddHydraulicComponents);
  }

  public static void gnStep15(VirtualModel vsum) {
    productDeveloper(vsum, CaseStudy2ProductModels::gnStep15LinkProductModel);
  }

  public static void gnStep16(VirtualModel vsum) {
    validationEngineer(vsum, CaseStudy2ValidationEnvironmentModels::gnStep16ConcretiseRequirements);
  }

  public static void gnStep17(VirtualModel vsum) {
    validationEngineer(vsum, CaseStudy2ValidationEnvironmentModels::gnStep17AddFunctions);
  }

  public static void gnStep18(VirtualModel vsum) {
    validationEngineer(vsum, CaseStudy2ValidationEnvironmentModels::gnStep18AdaptArchitecture);
  }

  public static void gnStep20(VirtualModel vsum) {
    validationEngineer(vsum, CaseStudy2ValidationEnvironmentModels::gnStep20LinkValidationModel);
  }

  public static void gnCloseGaps(VirtualModel vsum) {
    validationEngineer(vsum, CaseStudy2ValidationEnvironmentModels::gnCloseBrakePressureGap);
  }

  /** Complete scenario 1 starting from the initial Gn-1 state. */
  public static void runScenario1(VirtualModel vsum, Path storageFolder) {
    deriveGeneration(vsum, storageFolder, GN, GN_MINUS_1);
    gnStep03(vsum);
    gnStep04(vsum);
    gnStep05(vsum);
    gnStep07(vsum);
    gnStep09(vsum);
    gnStep11(vsum);
    gnStep13(vsum);
    gnStep15(vsum);
    gnStep16(vsum);
    gnStep17(vsum);
    gnStep18(vsum);
    gnStep20(vsum);
    gnCloseGaps(vsum);
  }

  // --- Scenario 2: Gn -> Gn+1 (recuperative braking)

  public static void gnPlus1Step08(VirtualModel vsum) {
    productDeveloper(vsum, CaseStudy2ProductModels::gnPlus1Step08AddRequirements);
    productDeveloper(vsum, CaseStudy2ProductModels::gnPlus1Step08AddFunctions);
    productDeveloper(vsum, CaseStudy2ProductModels::gnPlus1Step08AddComponents);
    productDeveloper(vsum, CaseStudy2ProductModels::gnPlus1Step08LinkProductModel);
  }

  public static void gnPlus1Step10(VirtualModel vsum) {
    validationEngineer(vsum, CaseStudy2ValidationEnvironmentModels::gnPlus1Step10ConcretiseRequirements);
    validationEngineer(vsum, CaseStudy2ValidationEnvironmentModels::gnPlus1Step10AddFunctions);
    validationEngineer(vsum, CaseStudy2ValidationEnvironmentModels::gnPlus1Step10AdaptArchitecture);
  }

  public static void gnPlus1CloseGaps(VirtualModel vsum) {
    validationEngineer(vsum, CaseStudy2ValidationEnvironmentModels::gnPlus1CloseGaps);
  }

  /** Complete scenario 2 starting from the state after scenario 1. */
  public static void runScenario2(VirtualModel vsum, Path storageFolder) {
    deriveGeneration(vsum, storageFolder, GN_PLUS_1, GN);
    gnPlus1Step08(vsum);
    gnPlus1Step10(vsum);
    gnPlus1CloseGaps(vsum);
  }
}
