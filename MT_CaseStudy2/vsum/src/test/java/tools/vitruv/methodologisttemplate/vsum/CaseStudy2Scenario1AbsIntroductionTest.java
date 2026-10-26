package tools.vitruv.methodologisttemplate.vsum;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tools.vitruv.methodologisttemplate.vsum.CaseStudy2TestSupport.names;
import static tools.vitruv.methodologisttemplate.vsum.CaseStudy2TestSupport.productView;
import static tools.vitruv.methodologisttemplate.vsum.CaseStudy2TestSupport.validationNeeds;
import static tools.vitruv.methodologisttemplate.vsum.CaseStudy2TestSupport.veArchitecture;
import static tools.vitruv.methodologisttemplate.vsum.CaseStudy2TestSupport.veFunctions;
import static tools.vitruv.methodologisttemplate.vsum.CaseStudy2TestSupport.veRequirements;
import static tools.vitruv.methodologisttemplate.vsum.CaseStudy2ValidationEnvironmentModels.componentNeed;
import static tools.vitruv.methodologisttemplate.vsum.CaseStudy2ValidationEnvironmentModels.configurationOf;
import static tools.vitruv.methodologisttemplate.vsum.CaseStudy2ValidationEnvironmentModels.functionNeed;
import static tools.vitruv.methodologisttemplate.vsum.CaseStudy2ValidationEnvironmentModels.requirement;
import static tools.vitruv.methodologisttemplate.vsum.CaseStudy2ValidationEnvironmentModels.restVehicleNeed;
import static tools.vitruv.methodologisttemplate.vsum.CaseStudy2Vsum.GN;
import static tools.vitruv.methodologisttemplate.vsum.CaseStudy2Vsum.GN_MINUS_1;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import cameo_architecture.BlockRepository;
import cameo_architecture.ValidationConfiguration;
import cameo_requirements.Requirement;
import cameo_requirements.RequirementClassification;
import cameo_requirements.RequirementsRepository;
import cameo_requirements.ValidationStatus;
import tools.vitruv.framework.views.View;
import tools.vitruv.framework.vsum.VirtualModel;

/**
 * Case Study 2, scenario 1 (slides 2-5): change from brake-system generation Gn-1 to Gn - introduction of ABS.
 *
 * <p>The test loads the initial state (snapshot 0: product Gn-1 and the validation environment for Gn-1) and then
 * walks through the 21 steps of slide 3 in code. Product developer and validation engineer change their own
 * models through views; Vitruvius applies CPRs "1" (product functions -&gt; VE requirements) and CPRs "2"
 * (product architecture -&gt; VE requirements) on every commit. After each step the test checks which validation
 * needs have been derived, and at the end that every need of Gn is covered by a VE requirement.
 */
class CaseStudy2Scenario1AbsIntroductionTest {

  @Test
  void introductionOfAbsIsReflectedInTheValidationEnvironment() {
    CaseStudy2TestSupport support = new CaseStudy2TestSupport("casestudy2-scenario1-vsum-output");
    // Snapshot 0 (initial state): product Gn-1 (without ABS) and the VE for Gn-1. This is the only snapshot
    // that is loaded - all steps of slide 3 are performed below in code.
    VirtualModel vsum = support.loadInitialState(CaseStudy2Snapshots.SCENARIO1_INITIAL_STATE);
    View products = productView(vsum);
    assertEquals(30, CaseStudy2Vsum.productRequirements(products, GN_MINUS_1).getRequirements().size());
    assertEquals(21, CaseStudy2Vsum.productFunctions(products, GN_MINUS_1).getFunctions().size());
    assertEquals(24, CaseStudy2Vsum.productArchitecture(products, GN_MINUS_1).getComponents().size());
    assertEquals(29, veRequirements(vsum).getRequirements().size());
    assertTrue(validationNeeds(vsum).isEmpty(), "the baseline generation must not derive validation needs");

    // Steps 1-2: project manager decides Gn, product developer takes over the system model of Gn-1.
    CaseStudy2Scenarios.deriveGeneration(vsum, support.storageFolder(), GN, GN_MINUS_1);
    products = productView(vsum);
    assertEquals(30, CaseStudy2Vsum.productRequirements(products, GN).getRequirements().size());
    assertEquals(21, CaseStudy2Vsum.productFunctions(products, GN).getFunctions().size());
    assertEquals(24, CaseStudy2Vsum.productArchitecture(products, GN).getComponents().size());
    assertTrue(validationNeeds(vsum).isEmpty(), "carried-over elements must not derive validation needs");
    assertTrue(veRequirements(vsum).getRequirements().stream()
        .allMatch(r -> r.getGenerations().equals(List.of(GN_MINUS_1, GN))),
        "the VE requirements of Gn-1 are carried over to Gn");
    assertTrue(veFunctions(vsum).getFunctions().stream().allMatch(f -> f.getGenerations().contains(GN)));
    BlockRepository blocks = veArchitecture(vsum);
    assertEquals(configurationOf(blocks, GN_MINUS_1).getParts(), configurationOf(blocks, GN).getParts(),
        "the Gn-1 test-bench configuration is carried over");
    assertEquals("Validation Configuration (Gn)", configurationOf(blocks, GN).getName());

    // Steps 3-4: new product requirements. No CPR between product and VE requirements (slide 2).
    CaseStudy2Scenarios.gnStep03(vsum);
    CaseStudy2Scenarios.gnStep04(vsum);
    assertTrue(validationNeeds(vsum).isEmpty(), "product requirements are not propagated directly");

    // Steps 5-6: function ABS (+ ABS monitoring) -> CPRs "1".
    CaseStudy2Scenarios.gnStep05(vsum);
    assertEquals(List.of("Validation need: product function 'ABS'",
        "Validation need: product function 'ABS-Überwachung'"), names(validationNeeds(vsum)));
    Requirement absNeed = functionNeed(veRequirements(vsum), "ABS");
    assertEquals("VN-Gn-F-01", absNeed.getId());
    assertEquals(List.of(GN), absNeed.getGenerations());
    assertEquals(ValidationStatus.IMAGINARY, absNeed.getStatus());
    assertEquals(RequirementClassification.VALIDATION_NEED_FROM_PRODUCT_FUNCTION, absNeed.getClassification());

    // Steps 7-8: wheel-speed function -> CPRs "1".
    CaseStudy2Scenarios.gnStep07(vsum);
    assertEquals(3, validationNeeds(vsum).size());
    assertEquals("VN-Gn-F-03", functionNeed(veRequirements(vsum), "Raddrehzahlen erfassen").getId());

    // Steps 9-10: wheel-speed sensors and the further ABS measured variables -> CPRs "2".
    CaseStudy2Scenarios.gnStep09(vsum);
    RequirementsRepository ve = veRequirements(vsum);
    for (String sensor : List.of("Raddrehzahlsensoren", "Beschleunigungssensor", "Bremsdrucksensor", "Gierratensensor",
        "Lenkwinkelsensor")) {
      Requirement need = componentNeed(ve, sensor);
      assertEquals(RequirementClassification.VALIDATION_NEED_FROM_PRODUCT_ARCHITECTURE, need.getClassification());
      assertTrue(need.getText().contains("measured variable of the sensor '" + sensor + "'"));
      assertTrue(need.isRestVehicleSimulationAffected());
    }
    Requirement restVehicle = restVehicleNeed(ve, GN);
    assertEquals("VN-Gn-RVS", restVehicle.getId());
    assertTrue(restVehicle.getText().endsWith(
        "Raddrehzahlsensoren; Beschleunigungssensor; Bremsdrucksensor; Gierratensensor; Lenkwinkelsensor;"),
        restVehicle.getText());
    assertEquals(9, validationNeeds(vsum).size());

    // Steps 11-12: ABS control unit -> CPRs "2" (connection of the control unit).
    CaseStudy2Scenarios.gnStep11(vsum);
    assertTrue(componentNeed(veRequirements(vsum), "elektrische Regeleinheit").getText()
        .contains("integration of the control unit"));
    assertEquals(10, validationNeeds(vsum).size());

    // Steps 13-14: valves, pump and adapted hydraulic circuit -> CPRs "2".
    CaseStudy2Scenarios.gnStep13(vsum);
    assertTrue(componentNeed(veRequirements(vsum), "Ventilblock").getText().contains("hydraulic / actuating component"));
    assertTrue(componentNeed(veRequirements(vsum), "Hydraulisch-Elektrischte-Regeleinheit").getText()
        .contains("Fahrbremse::Hydraulisch-Elektrischte-Regeleinheit"));
    assertEquals(13, validationNeeds(vsum).size());
    assertFalse(restVehicleNeed(veRequirements(vsum), GN).getText().contains("Ventilblock"),
        "hydraulic components do not require an extension of the remaining vehicle simulation");

    // Step 15: product-internal relations - no CPRs inside one system model.
    CaseStudy2Scenarios.gnStep15(vsum);
    assertEquals(13, validationNeeds(vsum).size());
    assertEquals(13, ValidationNeedCoverage.uncoveredValidationNeeds(veRequirements(vsum), GN).size());

    // Step 16: validation engineer concretises the needs into the VE requirements of V87 (slide 5).
    CaseStudy2Scenarios.gnStep16(vsum);
    ve = veRequirements(vsum);
    assertEquals(List.of(GN), requirement(ve, "6.1").getGenerations());
    assertEquals(Set.of(componentNeed(ve, "Beschleunigungssensor"), componentNeed(ve, "Gierratensensor"),
        componentNeed(ve, "Lenkwinkelsensor")), Set.copyOf(requirement(ve, "6.6").getReferences()));
    // V87 has pressure sensors in the Gn configuration but no VE requirement for brake pressure:
    assertEquals(List.of("Validation need: product component 'Bremsdrucksensor'"),
        names(ValidationNeedCoverage.uncoveredValidationNeeds(ve, GN)));

    // Steps 17, 18, 20: VE functions, VE architecture of the Gn configuration and their relations.
    CaseStudy2Scenarios.gnStep17(vsum);
    CaseStudy2Scenarios.gnStep18(vsum);
    CaseStudy2Scenarios.gnStep20(vsum);
    assertEquals(32, veFunctions(vsum).getFunctions().size());
    ValidationConfiguration gnConfiguration = configurationOf(veArchitecture(vsum), GN);
    assertEquals("Imaginary Validation Configuration (Gn) - ABS & ECU Integration", gnConfiguration.getName());
    assertTrue(gnConfiguration.getParts().stream().anyMatch(b -> "Imaginary Wheel Speed Sensor".equals(b.getName())));
    assertFalse(gnConfiguration.getParts().stream().anyMatch(b -> "Brake Pedal".equals(b.getName())));
    assertTrue(configurationOf(veArchitecture(vsum), GN_MINUS_1).getParts().stream()
        .anyMatch(b -> "Brake Pedal".equals(b.getName())), "the Gn-1 configuration is not changed");

    // Gap closed by the validation engineer: brake-pressure measurement.
    CaseStudy2Scenarios.gnCloseGaps(vsum);
    assertTrue(ValidationNeedCoverage.uncoveredValidationNeeds(veRequirements(vsum), GN).isEmpty(),
        ValidationNeedCoverage.report(veRequirements(vsum), GN));
    assertTrue(ValidationNeedCoverage.validationNeeds(veRequirements(vsum), GN_MINUS_1).isEmpty(),
        "Gn-1 is not affected by the ABS needs");
  }
}
