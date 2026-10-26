package tools.vitruv.methodologisttemplate.vsum;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tools.vitruv.methodologisttemplate.vsum.CaseStudy2TestSupport.veRequirements;
import static tools.vitruv.methodologisttemplate.vsum.CaseStudy2ValidationEnvironmentModels.componentNeed;
import static tools.vitruv.methodologisttemplate.vsum.CaseStudy2ValidationEnvironmentModels.functionNeed;
import static tools.vitruv.methodologisttemplate.vsum.CaseStudy2ValidationEnvironmentModels.requirement;
import static tools.vitruv.methodologisttemplate.vsum.CaseStudy2Vsum.GN;
import static tools.vitruv.methodologisttemplate.vsum.CaseStudy2Vsum.GN_MINUS_1;
import static tools.vitruv.methodologisttemplate.vsum.CaseStudy2Vsum.GN_PLUS_1;
import static tools.vitruv.methodologisttemplate.vsum.CaseStudy2Vsum.PRODUCT_ROOTS;

import java.util.List;

import org.eclipse.emf.ecore.util.EcoreUtil;
import org.junit.jupiter.api.Test;

import cameo_requirements.Requirement;
import cameo_requirements.RequirementsRepository;
import tools.vitruv.framework.vsum.VirtualModel;

/**
 * Case Study 2: changes and deletions in one generation must not break the validation environment of the other
 * generations. Unlike the 1:1 delete reactions of Case Study 1, CPRs "1"/"2" only remove the generation tag and
 * flag the need for review. All tests start from snapshot 0 of scenario 2 (state after scenario 1).
 */
class CaseStudy2GenerationScopedChangesTest {

  private VirtualModel vsumWithGnPlus1Derived(CaseStudy2TestSupport support) {
    VirtualModel vsum = support.loadInitialState(CaseStudy2Snapshots.SCENARIO2_INITIAL_STATE);
    CaseStudy2Scenarios.deriveGeneration(vsum, support.storageFolder(), GN_PLUS_1, GN);
    return vsum;
  }

  @Test
  void removingAComponentInGnPlus1KeepsTheValidationNeedOfGn() {
    CaseStudy2TestSupport support = new CaseStudy2TestSupport("casestudy2-generation-scoped-delete");
    VirtualModel vsum = vsumWithGnPlus1Derived(support);
    int requirementsBefore = veRequirements(vsum).getRequirements().size();

    // What-if: in Gn+1 the brake pressure is estimated instead of measured.
    CaseStudy2Vsum.modify(vsum, PRODUCT_ROOTS, v -> EcoreUtil.delete(CaseStudy2ProductModels.component(
        CaseStudy2Vsum.productArchitecture(v, GN_PLUS_1), "Bremsdrucksensor")));

    RequirementsRepository ve = veRequirements(vsum);
    Requirement need = componentNeed(ve, "Bremsdrucksensor");
    assertEquals(List.of(GN), need.getGenerations(), "only Gn+1 is removed from the need");
    assertFalse(need.isReviewRequired());
    assertEquals(requirementsBefore, ve.getRequirements().size(), "nothing is deleted in the VE");
    assertFalse(ValidationNeedCoverage.validationNeeds(ve, GN_PLUS_1).contains(need));
    assertTrue(ValidationNeedCoverage.uncoveredValidationNeeds(ve, GN).isEmpty());
    assertTrue(requirement(ve, "6.8").getGenerations().contains(GN));
  }

  @Test
  void removingAFunctionFromItsOnlyGenerationFlagsTheNeedForReview() {
    CaseStudy2TestSupport support = new CaseStudy2TestSupport("casestudy2-generation-scoped-delete-last");
    VirtualModel vsum = support.loadInitialState(CaseStudy2Snapshots.SCENARIO2_INITIAL_STATE);

    CaseStudy2Vsum.modify(vsum, PRODUCT_ROOTS, v -> EcoreUtil.delete(CaseStudy2ProductModels.function(
        CaseStudy2Vsum.productFunctions(v, GN), "ABS-Überwachung")));

    Requirement need = functionNeed(veRequirements(vsum), "ABS-Überwachung");
    assertTrue(need.getGenerations().isEmpty());
    assertTrue(need.isReviewRequired(), "the validation engineer has to decide whether 6.3 is still needed");
  }

  @Test
  void renamingAFunctionFlagsItsValidationNeedForReview() {
    CaseStudy2TestSupport support = new CaseStudy2TestSupport("casestudy2-rename");
    VirtualModel vsum = vsumWithGnPlus1Derived(support);

    CaseStudy2Vsum.modify(vsum, PRODUCT_ROOTS, v -> CaseStudy2ProductModels.function(
        CaseStudy2Vsum.productFunctions(v, GN_PLUS_1), "ABS").setName("ABS-Regelung"));

    Requirement need = functionNeed(veRequirements(vsum), "ABS");
    assertTrue(need.isReviewRequired());
    assertTrue(need.getText().contains("renamed from 'ABS' to 'ABS-Regelung'"));
    assertEquals(List.of(GN, GN_PLUS_1), need.getGenerations(), "a rename is not a removal");
    assertFalse(componentNeed(veRequirements(vsum), "Raddrehzahlsensoren").isReviewRequired());
  }

  @Test
  void anElementWithANewOwnerIsNotTreatedAsCarriedOver() {
    CaseStudy2TestSupport support = new CaseStudy2TestSupport("casestudy2-identity");
    VirtualModel vsum = vsumWithGnPlus1Derived(support);
    CaseStudy2Vsum.modify(vsum, PRODUCT_ROOTS, CaseStudy2ProductModels::gnPlus1Step08AddComponents);

    RequirementsRepository ve = veRequirements(vsum);
    long controlUnitNeeds = ve.getRequirements().stream()
        .filter(r -> "Validation need: product component 'Steuergerät'".equals(r.getName())).count();
    assertEquals(1, controlUnitNeeds, "only the new Rekuperationseinrichtung::Steuergerät derives a need");
    assertEquals(List.of(GN_PLUS_1), componentNeed(ve, "Steuergerät").getGenerations());
    assertTrue(ValidationNeedCoverage.validationNeeds(ve, GN_MINUS_1).isEmpty());
  }
}
