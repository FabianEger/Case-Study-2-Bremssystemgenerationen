package tools.vitruv.methodologisttemplate.vsum;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tools.vitruv.methodologisttemplate.vsum.CaseStudy2TestSupport.names;
import static tools.vitruv.methodologisttemplate.vsum.CaseStudy2TestSupport.validationNeeds;
import static tools.vitruv.methodologisttemplate.vsum.CaseStudy2TestSupport.veArchitecture;
import static tools.vitruv.methodologisttemplate.vsum.CaseStudy2TestSupport.veFunctions;
import static tools.vitruv.methodologisttemplate.vsum.CaseStudy2TestSupport.veRequirements;
import static tools.vitruv.methodologisttemplate.vsum.CaseStudy2ValidationEnvironmentModels.componentNeed;
import static tools.vitruv.methodologisttemplate.vsum.CaseStudy2ValidationEnvironmentModels.configurationOf;
import static tools.vitruv.methodologisttemplate.vsum.CaseStudy2ValidationEnvironmentModels.function;
import static tools.vitruv.methodologisttemplate.vsum.CaseStudy2ValidationEnvironmentModels.functionNeed;
import static tools.vitruv.methodologisttemplate.vsum.CaseStudy2ValidationEnvironmentModels.requirement;
import static tools.vitruv.methodologisttemplate.vsum.CaseStudy2ValidationEnvironmentModels.restVehicleNeed;
import static tools.vitruv.methodologisttemplate.vsum.CaseStudy2Vsum.GN;
import static tools.vitruv.methodologisttemplate.vsum.CaseStudy2Vsum.GN_MINUS_1;
import static tools.vitruv.methodologisttemplate.vsum.CaseStudy2Vsum.GN_PLUS_1;

import java.util.List;

import org.junit.jupiter.api.Test;

import cameo_architecture.BlockRepository;
import cameo_architecture.ValidationConfiguration;
import cameo_requirements.Requirement;
import cameo_requirements.RequirementsRepository;
import tools.vitruv.framework.vsum.VirtualModel;

/**
 * Case Study 2, scenario 2 (slides 6-8): change from Gn (ABS) to Gn+1 - introduction of recuperative braking.
 *
 * <p>Starts from snapshot 0 of this scenario (the V-SUM state after scenario 1) and performs all steps in code.
 * Checks that taking over Gn as basis of Gn+1 only carries the generation tags over, that CPRs "1"/"2" derive validation needs exactly for the new functions and components
 * (identified by name <em>and</em> owner), and which needs V87 leaves uncovered.
 */
class CaseStudy2Scenario2RecuperativeBrakingTest {

  private static final List<String> NEW_COMPONENT_NEEDS = List.of(
      "Validation need: product component 'Rekuperationseinrichtung'",
      "Validation need: product component 'Gaspedal'",
      "Validation need: product component 'Leistungselektronik'",
      "Validation need: product component 'Generator'",
      "Validation need: product component 'Fahrzeugbatterie'",
      "Validation need: product component 'Steuergerät'",
      "Validation need: product component 'Steuerventil'");

  @Test
  void recuperativeBrakingIsReflectedInTheValidationEnvironment() {
    CaseStudy2TestSupport support = new CaseStudy2TestSupport("casestudy2-scenario2-vsum-output");
    // Snapshot 0 (initial state): product Gn-1 and Gn (ABS) and the validation environment adapted to Gn, i.e. the
    // result of scenario 1. This is the only snapshot that is loaded - steps 6-10 of slide 6 are performed in code.
    VirtualModel vsum = support.loadInitialState(CaseStudy2Snapshots.SCENARIO2_INITIAL_STATE);
    int needsOfGn = validationNeeds(vsum).size();
    int veRequirementsAfterGn = veRequirements(vsum).getRequirements().size();
    assertEquals(13, needsOfGn);

    // Steps 6-7: new generation Gn+1 on the basis of Gn.
    CaseStudy2Scenarios.deriveGeneration(vsum, support.storageFolder(), GN_PLUS_1, GN);
    RequirementsRepository ve = veRequirements(vsum);
    assertEquals(veRequirementsAfterGn, ve.getRequirements().size(), "copying Gn must not derive new needs");
    assertTrue(validationNeeds(vsum).stream().allMatch(n -> n.getGenerations().equals(List.of(GN, GN_PLUS_1))),
        "all ABS validation needs still apply to Gn+1");
    // Generation-set derivation: the ABS requirements now also carry Gn+1 (in V87 4.4/4.7/4.8 miss Gn+1).
    assertEquals(List.of(GN, GN_PLUS_1), requirement(ve, "4.4").getGenerations());
    assertEquals(List.of(GN, GN_PLUS_1), requirement(ve, "4.12").getGenerations());
    assertEquals(List.of(GN_MINUS_1, GN, GN_PLUS_1), requirement(ve, "4.7").getGenerations());
    assertEquals(List.of(GN, GN_PLUS_1), function(veFunctions(vsum), "Simulation of yaw rate").getGenerations());
    BlockRepository blocks = veArchitecture(vsum);
    assertEquals(configurationOf(blocks, GN).getParts(), configurationOf(blocks, GN_PLUS_1).getParts(),
        "the ABS test-bench configuration of Gn is the starting point for Gn+1");
    assertTrue(ValidationNeedCoverage.uncoveredValidationNeeds(ve, GN_PLUS_1).isEmpty());

    // Step 8: product developer adds requirements, functions and components for recuperative braking.
    CaseStudy2Scenarios.gnPlus1Step08(vsum);
    ve = veRequirements(vsum);
    List<Requirement> newNeeds = validationNeeds(vsum).stream()
        .filter(n -> n.getGenerations().equals(List.of(GN_PLUS_1))).toList();
    assertEquals(2 + NEW_COMPONENT_NEEDS.size() + 1, newNeeds.size(), names(newNeeds).toString());
    assertEquals(List.of(GN_PLUS_1), functionNeed(ve, "Rekuperieren").getGenerations());
    assertEquals(List.of(GN_PLUS_1), functionNeed(ve, "Bremsbefehle erhalten").getGenerations());
    for (String need : NEW_COMPONENT_NEEDS) {
      assertTrue(names(newNeeds).contains(need), need);
    }
    // Same name as the parking-brake control unit (since Gn-1) but a different owner -> new component.
    assertTrue(componentNeed(ve, "Steuergerät").getText().contains("'Rekuperationseinrichtung::Steuergerät'"));
    assertTrue(componentNeed(ve, "Generator").getText().contains("electric drive train"));
    Requirement restVehicle = restVehicleNeed(ve, GN_PLUS_1);
    assertTrue(restVehicle.getText().endsWith("Gaspedal; Leistungselektronik; Generator; Fahrzeugbatterie;"),
        restVehicle.getText());
    assertEquals(List.of(GN, GN_PLUS_1), restVehicleNeed(ve, GN).getGenerations());
    assertEquals(newNeeds.size(), ValidationNeedCoverage.uncoveredValidationNeeds(ve, GN_PLUS_1).size());

    // Step 10: validation engineer adapts the VE (blended braking 4.9, torque split, e-drive components).
    CaseStudy2Scenarios.gnPlus1Step10(vsum);
    ve = veRequirements(vsum);
    assertEquals(List.of(GN_PLUS_1), requirement(ve, "4.9").getGenerations());
    ValidationConfiguration configuration = configurationOf(veArchitecture(vsum), GN_PLUS_1);
    assertTrue(configuration.getParts().stream().anyMatch(b -> "Motor (Generator)".equals(b.getName())));
    assertFalse(configurationOf(veArchitecture(vsum), GN).getParts().stream()
        .anyMatch(b -> "Motor (Generator)".equals(b.getName())), "the Gn configuration is not changed");
    // V87 does not validate one-pedal driving (23.2.11.1) nor brake commands of assistance systems (23.6.2):
    assertEquals(List.of("Validation need: product function 'Bremsbefehle erhalten'",
        "Validation need: product component 'Gaspedal'", "Validation need: product component 'Steuerventil'"),
        names(ValidationNeedCoverage.uncoveredValidationNeeds(ve, GN_PLUS_1)));

    // Gaps closed by the validation engineer.
    CaseStudy2Scenarios.gnPlus1CloseGaps(vsum);
    ve = veRequirements(vsum);
    assertTrue(ValidationNeedCoverage.uncoveredValidationNeeds(ve, GN_PLUS_1).isEmpty(),
        ValidationNeedCoverage.report(ve, GN_PLUS_1));
    assertTrue(ValidationNeedCoverage.uncoveredValidationNeeds(ve, GN).isEmpty());
    assertFalse(requirement(ve, "4.16").getGenerations().contains(GN), "Gn does not need one-pedal driving");
  }
}
