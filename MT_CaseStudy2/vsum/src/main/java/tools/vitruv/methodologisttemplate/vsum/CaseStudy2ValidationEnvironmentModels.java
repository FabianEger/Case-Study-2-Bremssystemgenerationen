package tools.vitruv.methodologisttemplate.vsum;

import static tools.vitruv.methodologisttemplate.vsum.CaseStudy2Vsum.GN;
import static tools.vitruv.methodologisttemplate.vsum.CaseStudy2Vsum.GN_MINUS_1;
import static tools.vitruv.methodologisttemplate.vsum.CaseStudy2Vsum.GN_PLUS_1;
import static tools.vitruv.methodologisttemplate.vsum.CaseStudy2Vsum.modelUri;
import static tools.vitruv.methodologisttemplate.vsum.CaseStudy2Vsum.veArchitecture;
import static tools.vitruv.methodologisttemplate.vsum.CaseStudy2Vsum.veFunctions;
import static tools.vitruv.methodologisttemplate.vsum.CaseStudy2Vsum.veRequirements;

import java.nio.file.Path;
import java.util.List;

import cameo_architecture.Block;
import cameo_architecture.BlockRepository;
import cameo_architecture.Cameo_architectureFactory;
import cameo_architecture.ValidationConfiguration;
import cameo_functions.Cameo_functionsFactory;
import cameo_functions.Function;
import cameo_functions.FunctionsRepository;
import cameo_requirements.Cameo_requirementsFactory;
import cameo_requirements.Requirement;
import cameo_requirements.RequirementClassification;
import cameo_requirements.RequirementsRepository;
import cameo_requirements.ValidationStatus;
import tools.vitruv.framework.views.CommittableView;

/**
 * Case Study 2: instances of the validation engineer view ("Cameo-Systemmodell der Validierungsumgebung").
 *
 * <p>Requirements, functions and blocks are taken from {@code Brake_system-in-the-Loop_Validierung_V87_PG.xml}
 * (package {@code 02_Validierung}): Id, name, text, Status, Classification and category of the
 * {@code TaggedRequirement}s, the {@code TaggedFunction}s with their dependency tree and «satisfy» relations,
 * and the blocks of the validation configurations per generation. Which elements belong to which step follows
 * the red boxes of slides 5 and 8. Generation tags are not copied from V87 - they are set for the generation
 * in which the element is introduced and then carried over by the CPRs.
 *
 * <p>Elements that are not in V87 but are needed to close the gaps found by the V-SUM are marked with
 * category {@value #PROPOSED}.
 */
public final class CaseStudy2ValidationEnvironmentModels {

  /** Category of VE elements proposed to close gaps of V87 (not contained in V87). */
  public static final String PROPOSED = "Proposed (not in V87)";

  private CaseStudy2ValidationEnvironmentModels() {
  }

  // ===========================================================================
  // Set-up and baseline Vn-1 (validation environment for Gn-1)
  // ===========================================================================

  /** Registers the (empty) VE requirements, functions and architecture models. */
  public static void registerValidationEnvironment(CommittableView view, Path storageFolder) {
    view.registerRoot(Cameo_requirementsFactory.eINSTANCE.createRequirementsRepository(),
        modelUri(storageFolder, "validation_environment.cameo_requirements"));
    view.registerRoot(Cameo_functionsFactory.eINSTANCE.createFunctionsRepository(),
        modelUri(storageFolder, "validation_environment.cameo_functions"));
    view.registerRoot(Cameo_architectureFactory.eINSTANCE.createBlockRepository(),
        modelUri(storageFolder, "validation_environment.cameo_architecture"));
  }

  /** VE of the existing test bench for Gn-1 (closed-loop variant without ABS, slide 10). */
  public static void populateVnMinus1(CommittableView view) {
    RequirementsRepository r = veRequirements(view);
    FunctionsRepository f = veFunctions(view);
    BlockRepository a = veArchitecture(view);

    for (String id : List.of("1.1", "1.2", "1.3", "1.4", "1.5", "1.6", "2.1", "2.2", "2.3", "2.4", "2.5", "2.6", "2.7",
        "2.8", "2.9", "4.1", "4.5", "4.6", "4.7", "4.8", "4.11", "5.1", "5.2", "5.3", "5.4", "5.5", "5.6", "6.2", "6.7")) {
      add(r, v87Requirement(id), GN_MINUS_1);
    }

    function(f, "Support Modular Test Bench Integration", null, GN_MINUS_1);
    function(f, "Enable Modular Brake system Integration", "Support Modular Test Bench Integration", GN_MINUS_1);
    function(f, "Safety Reliable operation", null, GN_MINUS_1);
    function(f, "Monitor Brake Disc Temperature", "Safety Reliable operation", GN_MINUS_1);
    function(f, "Ensure Mechanical stability", "Safety Reliable operation", GN_MINUS_1);
    function(f, "Provide Emergency Shutdown", "Safety Reliable operation", GN_MINUS_1);
    function(f, "Support Concrete Test Case Execution", null, GN_MINUS_1);
    function(f, "Enable Emergency Braking Tests", "Support Concrete Test Case Execution", GN_MINUS_1, r, "1.2", "4.7");
    function(f, "Support Repeated Braking Cycles", "Support Concrete Test Case Execution", GN_MINUS_1);
    function(f, "Simulation of Vehicle Dynamics", null, GN_MINUS_1);
    function(f, "Simulate Driver", "Simulation of Vehicle Dynamics", GN_MINUS_1);
    function(f, "Simulate Driving Resistances", "Simulation of Vehicle Dynamics", GN_MINUS_1);
    function(f, "Generate Rotational Speed & Torque", "Simulation of Vehicle Dynamics", GN_MINUS_1);
    function(f, "Transmit Rotational speed & Torque", "Simulation of Vehicle Dynamics", GN_MINUS_1);
    function(f, "Enable Brake Actuation", "Simulation of Vehicle Dynamics", GN_MINUS_1);
    function(f, "Generate Simulated Brake Pedal Position", "Enable Brake Actuation", GN_MINUS_1);
    function(f, "Control Brake Pressure for Hydraulic Braking", "Enable Brake Actuation", GN_MINUS_1, r, "1.1");
    function(f, "Measure , Record Data & Control system behaviour", null, GN_MINUS_1);
    String measure = "Measure , Record Data & Control system behaviour";
    function(f, "Measure Rotational Speed of shaft", measure, GN_MINUS_1);
    function(f, "Measure Braking Torque of the shaft", measure, GN_MINUS_1);
    function(f, "Monitor System Operating state", measure, GN_MINUS_1);
    function(f, "Control Rotational Direction", measure, GN_MINUS_1);
    function(f, "Control Rotational speed", measure, GN_MINUS_1);
    function(f, "Control Torque of the shaft", measure, GN_MINUS_1);
    function(f, "Control Braking Torque", measure, GN_MINUS_1, r, "1.1", "1.2", "4.8");
    function(f, "Record Measurement data", measure, GN_MINUS_1);

    ValidationConfiguration configuration = configuration(a, "Validation Configuration (Gn-1)", GN_MINUS_1);
    for (String name : List.of("Mounting Plate", "Electric Machine", "Bellow Clutch Assembly", "Measurement Technique",
        "Hollow shaft adapter", "Ring Clamping Element", "Main Shaft", "Smaller Bearing Assembly",
        "Bigger bearing Assembly", "Brake Adapter", "Brake Disc", "Brake Caliper Holder", "Brake Caliper",
        "Friction Pad", "Friction Pad holder", "Brake Pedal", "Brake Booster", "Master Cylinder",
        "Hydraulic Brake Pressure Generator", "Driver", "Environment", "Remaining Driving Vehicle Simulation")) {
      configuration.getParts().add(block(a, name, false));
    }
    allocate(a, f, "Electric Machine", "Generate Rotational Speed & Torque", "Control Rotational speed",
        "Control Rotational Direction", "Simulate Driving Resistances", "Control Braking Torque");
    allocate(a, f, "Measurement Technique", "Measure Braking Torque of the shaft", "Measure Rotational Speed of shaft",
        "Record Measurement data");
    allocate(a, f, "Main Shaft", "Transmit Rotational speed & Torque");
    allocate(a, f, "Bellow Clutch Assembly", "Ensure Mechanical stability");
    allocate(a, f, "Brake Adapter", "Enable Modular Brake system Integration");
    allocate(a, f, "Hydraulic Brake Pressure Generator", "Enable Brake Actuation", "Enable Emergency Braking Tests",
        "Support Repeated Braking Cycles");
    allocate(a, f, "Driver", "Simulate Driver");
  }

  // ===========================================================================
  // Scenario 1 (slides 3 and 5): validation engineer adapts the VE for Gn (ABS)
  // ===========================================================================

  /**
   * Step 16: checks and concretises the validation needs identified by CPRs "1" and "2" into the VE
   * requirements of V87 (red boxes on slide 5). Every VE requirement references the needs it covers.
   */
  public static void gnStep16ConcretiseRequirements(CommittableView view) {
    RequirementsRepository r = veRequirements(view);
    covers(add(r, v87Requirement("4.4"), GN), functionNeed(r, "ABS"));
    covers(add(r, v87Requirement("4.12"), GN), componentNeed(r, "Hydraulisch-Elektrischte-Regeleinheit"),
        componentNeed(r, "Ventilblock"), componentNeed(r, "Hydraulikpumpe"));
    covers(add(r, v87Requirement("4.13"), GN), componentNeed(r, "Raddrehzahlsensoren"), restVehicleNeed(r, GN));
    covers(add(r, v87Requirement("4.14"), GN), restVehicleNeed(r, GN));
    covers(add(r, v87Requirement("4.15"), GN), functionNeed(r, "ABS"));
    covers(add(r, v87Requirement("5.7"), GN), componentNeed(r, "elektrische Regeleinheit"));
    covers(add(r, v87Requirement("6.1"), GN), functionNeed(r, "Raddrehzahlen erfassen"),
        componentNeed(r, "Raddrehzahlsensoren"));
    covers(add(r, v87Requirement("6.3"), GN), functionNeed(r, "ABS-Überwachung"));
    covers(add(r, v87Requirement("6.6"), GN), componentNeed(r, "Beschleunigungssensor"),
        componentNeed(r, "Gierratensensor"), componentNeed(r, "Lenkwinkelsensor"));
  }

  /** Step 17: VE functions for the additional measured variables and for examining the ABS control (slide 5). */
  public static void gnStep17AddFunctions(CommittableView view) {
    FunctionsRepository f = veFunctions(view);
    RequirementsRepository r = veRequirements(view);
    function(f, "Simulation of wheel speed", "Simulation of Vehicle Dynamics", GN, r, "4.13");
    function(f, "Simulate Wheel slip", "Simulation of wheel speed", GN, r, "4.4");
    function(f, "Simulation of wheel accleration", "Simulation of Vehicle Dynamics", GN, r, "6.6");
    function(f, "Simulation of yaw rate", "Simulation of Vehicle Dynamics", GN, r, "6.6");
    function(f, "Simulation of steering angle", "Simulation of Vehicle Dynamics", GN, r, "6.6");
    function(f, "Enable Tests with varying frction conditions to validate the ABS function",
        "Support Concrete Test Case Execution", GN, r, "4.4", "4.15");
    satisfies(f, r, "Measure Rotational Speed of shaft", "6.1");
    satisfies(f, r, "Record Measurement data", "6.3");
  }

  /**
   * Step 18: measurement channels for the wheel speeds and interfaces to the ABS control unit in the Gn
   * configuration ("Imaginary Validation Configuration (Gn) - ABS &amp; ECU Integration" in V87).
   */
  public static void gnStep18AdaptArchitecture(CommittableView view) {
    BlockRepository a = veArchitecture(view);
    ValidationConfiguration configuration = configurationOf(a, GN);
    configuration.setName("Imaginary Validation Configuration (Gn) - ABS & ECU Integration");
    replace(configuration, a, "Brake Pedal", "Brake Pedal for ABS");
    replace(configuration, a, "Brake Booster", "Brake Booster for ABS");
    replace(configuration, a, "Master Cylinder", "Master Cylinder for ABS");
    for (String name : List.of("Imaginary ABS ECU", "Imaginary Wheel Speed Sensor", "Imaginary Tone Ring",
        "Imaginary Hydraulic Modulator", "Imaginary Pressure Sensor 1", "Imaginary Pressure Sensors 2",
        "Longitudinal Sensor")) {
      configuration.getParts().add(block(a, name, true));
    }
  }

  /** Step 20: VE-internal relations (allocation of the new VE functions to the new blocks). */
  public static void gnStep20LinkValidationModel(CommittableView view) {
    BlockRepository a = veArchitecture(view);
    FunctionsRepository f = veFunctions(view);
    allocate(a, f, "Imaginary Wheel Speed Sensor", "Simulation of wheel speed");
    allocate(a, f, "Longitudinal Sensor", "Simulation of wheel accleration");
    allocate(a, f, "Remaining Driving Vehicle Simulation", "Simulation of yaw rate", "Simulation of steering angle",
        "Simulate Wheel slip");
    allocate(a, f, "Imaginary ABS ECU", "Enable Tests with varying frction conditions to validate the ABS function");
  }

  /**
   * Gap found by the V-SUM in V87: the brake-pressure sensor of Gn has a counterpart in the VE architecture
   * (Imaginary Pressure Sensor 1/2) but no VE requirement or function. The validation engineer closes it.
   */
  public static void gnCloseBrakePressureGap(CommittableView view) {
    RequirementsRepository r = veRequirements(view);
    Requirement requirement = veReq("6.8", "Brake Pressure Measurement",
        "The validation environment shall measure the hydraulic brake pressure before and after the ABS modulation and"
            + " provide it to the ABS controller.",
        ValidationStatus.IMAGINARY, RequirementClassification.UNCLASSIFIED, PROPOSED);
    covers(add(r, requirement, GN), componentNeed(r, "Bremsdrucksensor"));
    FunctionsRepository f = veFunctions(view);
    function(f, "Measure Brake Pressure", "Measure , Record Data & Control system behaviour", GN, r, "6.8");
    allocate(veArchitecture(view), f, "Imaginary Pressure Sensor 1", "Measure Brake Pressure");
    allocate(veArchitecture(view), f, "Imaginary Pressure Sensors 2", "Measure Brake Pressure");
  }

  // ===========================================================================
  // Scenario 2 (slides 6 and 8): validation engineer adapts the VE for Gn+1 (recuperative braking)
  // ===========================================================================

  /** Step 10 (requirements): blended braking 4.9 covers the needs of the recuperation device. */
  public static void gnPlus1Step10ConcretiseRequirements(CommittableView view) {
    RequirementsRepository r = veRequirements(view);
    covers(add(r, v87Requirement("4.9"), GN_PLUS_1), functionNeed(r, "Rekuperieren"),
        componentNeed(r, "Rekuperationseinrichtung"), componentNeed(r, "Generator"),
        componentNeed(r, "Leistungselektronik"), componentNeed(r, "Fahrzeugbatterie"),
        componentNeed(r, "Steuergerät"), restVehicleNeed(r, GN_PLUS_1));
  }

  /** Step 10 (functions): interplay and torque split between recuperative and hydraulic braking (slide 8). */
  public static void gnPlus1Step10AddFunctions(CommittableView view) {
    FunctionsRepository f = veFunctions(view);
    RequirementsRepository r = veRequirements(view);
    function(f, "Enable Tests with conditions to validate the recuperative braking",
        "Support Concrete Test Case Execution", GN_PLUS_1, r, "4.9");
    function(f, "Split Braking Torque Between ABS & Recuperative Braking",
        "Enable Tests with conditions to validate the recuperative braking", GN_PLUS_1, r, "4.9");
    function(f, "Transition Between Recuperative & ABS braking",
        "Enable Tests with conditions to validate the recuperative braking", GN_PLUS_1, r, "4.9");
    satisfies(f, r, "Control Braking Torque", "4.9");
  }

  /** Step 10 (architecture): physical integration or virtual representation of the e-drive components (slide 8). */
  public static void gnPlus1Step10AdaptArchitecture(CommittableView view) {
    BlockRepository a = veArchitecture(view);
    FunctionsRepository f = veFunctions(view);
    ValidationConfiguration configuration = configurationOf(a, GN_PLUS_1);
    configuration.setName("Imaginary Validation Configuration (Gn+1) ABS + Recuperative Braking integration");
    for (String name : List.of("Battery", "Inverter / Controller", "Motor (Generator)", "Brake Control Unit",
        "Brake Pedal Sensors")) {
      configuration.getParts().add(block(a, name, true));
    }
    allocate(a, f, "Motor (Generator)", "Split Braking Torque Between ABS & Recuperative Braking");
    allocate(a, f, "Brake Control Unit", "Transition Between Recuperative & ABS braking");
  }

  /**
   * Gaps found by the V-SUM in V87 for Gn+1: one-pedal driving via the accelerator pedal (23.2.11.1) and the
   * brake commands of assistance systems via the externally controllable brake booster (23.6.2).
   */
  public static void gnPlus1CloseGaps(CommittableView view) {
    RequirementsRepository r = veRequirements(view);
    FunctionsRepository f = veFunctions(view);
    covers(add(r, veReq("4.16", "One-Pedal Driving Actuation",
        "The validation environment shall provide a (simulated) accelerator pedal position so that recuperative braking"
            + " operated via the accelerator pedal (one-pedal driving) can be validated.",
        ValidationStatus.IMAGINARY, RequirementClassification.UNCLASSIFIED, PROPOSED), GN_PLUS_1),
        componentNeed(r, "Gaspedal"));
    covers(add(r, veReq("5.8", "External Brake Request Interface",
        "The validation environment shall be able to inject brake requests of assistance systems into the brake system"
            + " under test (externally controllable brake booster).",
        ValidationStatus.IMAGINARY, RequirementClassification.UNCLASSIFIED, PROPOSED), GN_PLUS_1),
        functionNeed(r, "Bremsbefehle erhalten"), componentNeed(r, "Steuerventil"));
    function(f, "Generate Simulated Accelerator Pedal Position", "Enable Brake Actuation", GN_PLUS_1, r, "4.16");
    function(f, "Simulate Assistance System Brake Requests", "Enable Brake Actuation", GN_PLUS_1, r, "5.8");
    allocate(veArchitecture(view), f, "Remaining Driving Vehicle Simulation",
        "Generate Simulated Accelerator Pedal Position", "Simulate Assistance System Brake Requests");
  }

  // ===========================================================================
  // V87 requirement catalogue
  // ===========================================================================

  /** Requirement of V87 (package 00_Requirements_validation_environment) without generation tags. */
  public static Requirement v87Requirement(String id) {
    switch (id) {
      case "1.1": return veReq("1.1", "BrakingWithVariousBrakeForce", "The braking system test bench shall support braking force or equivalent braking torque levels in a controlled and repeatable manner, enabling evaluation of braking behavior under different braking intensities across all operating modes.", ValidationStatus.REAL, RequirementClassification.DERIVED_FROM_TEST_BENCH_ITSELF, "Variables in braking", "General");
      case "1.2": return veReq("1.2", "EmergencyBrakeFrom50", "The system shall allow emergency braking operations decelerating a rotating brake component from 50 km/h to 0 km/h using the maximum available brake force", ValidationStatus.REAL, RequirementClassification.DERIVED_FROM_TEST_BENCH_ITSELF, "General");
      case "1.3": return veReq("1.3", "RepeatedBrakingWithoutStopingTheRotationOfTheBrakeDisc", "The system shall support repeated braking cycles with variable brake force without the rotating brake component coming to a complete stop.", ValidationStatus.REAL, RequirementClassification.DERIVED_FROM_TEST_BENCH_ITSELF, "Variables in braking", "General");
      case "1.4": return veReq("1.4", "BrakingOperationsWithForwardAndReverseRotation", "The system shall allow braking operations with both forward and reverse rotation, including the reversal of the rotational direction of the brake component.", ValidationStatus.REAL, RequirementClassification.DERIVED_FROM_TEST_BENCH_ITSELF, "Variables in braking", "General");
      case "1.5": return veReq("1.5", "Integrated Parking Brake Functionality", "Parking brake mechanism can be easily integrated into the drum brake assembly without requiring additional systems", ValidationStatus.IMAGINARY, RequirementClassification.DERIVED_FROM_PRODUCT_TEST_CASE, "Drum Brake");
      case "1.6": return veReq("1.6", "High Braking Force at Low Pressure", "Drum brakes can generate high braking force with relatively low hydraulic pressure due to the self-energizing effect.", ValidationStatus.IMAGINARY, RequirementClassification.DERIVED_FROM_PRODUCT_TEST_CASE, "Drum Brake");
      case "2.1": return veReq("2.1", "NoFailureUnderMaxLoad", "All load-bearing components shall be designed to withstand the maximum expected operational loads without failure or critical crack formation.", ValidationStatus.REAL, RequirementClassification.DERIVED_FROM_TEST_BENCH_ITSELF, "General");
      case "2.2": return veReq("2.2", "NoFailureUnderMaxSpeed", "All rotating components shall be designed to withstand the maximum operating speed without structural failure, excessive vibration, or critical resonance.", ValidationStatus.REAL, RequirementClassification.DERIVED_FROM_TEST_BENCH_ITSELF, "General");
      case "2.3": return veReq("2.3", "NoCriticalEigenfrequencies", "The system shall prevent resonance effects from vibrations induced by braking forces or other actuators such as electric machines.", ValidationStatus.REAL, RequirementClassification.DERIVED_FROM_TEST_BENCH_ITSELF, "General", "Bearing");
      case "2.4": return veReq("2.4", "Bearing Arrangement for Operational Loads", "The bearing arrangement should have maximize tilting rigidity and effectively absorb external forces and torques applied outside the bearing span (from the electric machine and braking system).", ValidationStatus.REAL, RequirementClassification.DERIVED_FROM_PRODUCT_TEST_CASE, "Bearing");
      case "2.5": return veReq("2.5", "BearingSufficientlyDimensioned", "The bearing concept shall support the radial and axial forces applied without introducing mechanical overconstraints or preload under normal operating conditions.", ValidationStatus.REAL, RequirementClassification.DERIVED_FROM_PRODUCT_TEST_CASE, "Bearing");
      case "2.6": return veReq("2.6", "ManufacturableByConventionalMethods", "All physical components of the system shall be manufacturable using conventional techniques such as turning, milling, laser cutting, and welding.", ValidationStatus.REAL, RequirementClassification.DERIVED_FROM_TEST_BENCH_ITSELF, "General");
      case "2.7": return veReq("2.7", "Thermal Expansion Accommodation", "It tends to increase preload as the shaft expands (due to heat), which can be beneficial in high-temperature environments where stability is required.", ValidationStatus.IMAGINARY, RequirementClassification.DERIVED_FROM_PRODUCT_TEST_CASE, "Bearing");
      case "2.8": return veReq("2.8", "Alignment Stability", "More tolerant of misalignment between shaft and housing.", ValidationStatus.REAL, RequirementClassification.UNCLASSIFIED, "Bearing");
      case "2.9": return veReq("2.9", "High Torsional Rigidity", "The spread of the contact lines (large effective span between load centers) gives the system greater resistance to tilting and bending moments.", ValidationStatus.REAL, RequirementClassification.UNCLASSIFIED, "Bearing");
      case "4.1": return veReq("4.1", "Road Load Simulation", "The second electric machine shall simulate realistic road load conditions including rolling resistance, aerodynamic drag, and gradient forces", ValidationStatus.IMAGINARY, RequirementClassification.DERIVED_FROM_PRODUCT_TEST_CASE, "Second Electric Machine");
      case "4.4": return veReq("4.4", "Dynamic Slip Control", "The validation environment shall enable operation over varying wheel slip conditions such as dry, wet, and icy roads", ValidationStatus.IMAGINARY, RequirementClassification.DERIVED_FROM_PRODUCT_TEST_CASE, "Test Bench along with ABS");
      case "4.5": return veReq("4.5", "Bidirectional Torque Capability", "The second electric machine shall apply both positive and negative torque to simulate uphill, downhill, and braking conditions.", ValidationStatus.IMAGINARY, RequirementClassification.DERIVED_FROM_PRODUCT_TEST_CASE, "Second Electric Machine");
      case "4.6": return veReq("4.6", "Safety and Overload Protection", "The second electric machine shall include protection mechanisms to prevent damage under overload, excessive slip, or unexpected torque spikes during test scenarios.", ValidationStatus.IMAGINARY, RequirementClassification.DERIVED_FROM_PRODUCT_TEST_CASE, "Second Electric Machine");
      case "4.7": return veReq("4.7", "Emergency Braking Scenarios", "The system shall simulate emergency braking maneuvers at varying speeds to assess ABS effectiveness in reducing stopping distance", ValidationStatus.IMAGINARY, RequirementClassification.DERIVED_FROM_PRODUCT_TEST_CASE, "Test Bench along with ABS");
      case "4.8": return veReq("4.8", "Brake Torque Modulation", "The test bench shall allow rapid modulation of brake torque to validate ABS response during lock-up prevention", ValidationStatus.IMAGINARY, RequirementClassification.DERIVED_FROM_PRODUCT_TEST_CASE, "Test Bench along with ABS");
      case "4.9": return veReq("4.9", "Blended Braking", "The validation environment shall validate blended braking with hydraulic ABS braking and recuperative braking.", ValidationStatus.IMAGINARY, RequirementClassification.DERIVED_FROM_PRODUCT_TEST_CASE, "Test Bench with ABS & Recuperative Braking");
      case "4.11": return veReq("4.11", "Friction Change due to Fluid Application", "It shall be possible to change the friction conditions by applying defined fluids (e.g., oil, water) without requiring extensive modifications to the setup.", ValidationStatus.REAL, RequirementClassification.DERIVED_FROM_PRODUCT_TEST_CASE, "Brake Friction Modification", "Brake Caliper");
      case "4.12": return veReq("4.12", "ABS Hydraulic Circuit Simulation", "The validation environment shall provide a hydraulic circuit including ABS components.", ValidationStatus.REAL, RequirementClassification.UNCLASSIFIED);
      case "4.13": return veReq("4.13", "Simulation of ABS Input Signals", "The validation environment shall provide signals required by the ABS controller.", ValidationStatus.REAL, RequirementClassification.UNCLASSIFIED);
      case "4.14": return veReq("4.14", "Closed-Loop Simulation", "The validation environment shall support closed-loop interaction between rest vehicle simulation and brake system with its behaviour.", ValidationStatus.REAL, RequirementClassification.UNCLASSIFIED);
      case "4.15": return veReq("4.15", "ABS Control Intervention", "The validation environment shall allow the ABS controller to actively modulate brake pressure.", ValidationStatus.REAL, RequirementClassification.UNCLASSIFIED);
      case "5.1": return veReq("5.1", "Easy Brake System Exchange", "The mechanical interface shall allow simple exchange of the brake system under test using standard tools, without major structural modifications.", ValidationStatus.REAL, RequirementClassification.DERIVED_FROM_PRODUCT_TEST_CASE, "Modular Platform", "Braking System");
      case "5.2": return veReq("5.2", "EasyExchangeBrakeCaliper", "The brake caliper must be mounted laterally to the brake disc to enable compatibility with different brake system geometries.", ValidationStatus.REAL, RequirementClassification.DERIVED_FROM_PRODUCT_TEST_CASE, "Modular Platform", "Brake Caliper");
      case "5.3": return veReq("5.3", "AdjustablePositionBrakeCaliper", "The brake caliper holder must allow quick repositioning of the brake caliper in all three spatial directions using elongated holes or equivalent adjustment features.", ValidationStatus.REAL, RequirementClassification.DERIVED_FROM_PRODUCT_TEST_CASE, "Modular Platform", "Brake Caliper");
      case "5.4": return veReq("5.4", "ShortLeverArmBrakeCaliperHolder", "The brake caliper holder shall minimize the lever arm relative to the braking force to reduce structural loads.", ValidationStatus.REAL, RequirementClassification.DERIVED_FROM_PRODUCT_TEST_CASE, "Brake Caliper");
      case "5.5": return veReq("5.5", "Compatibility with Smaller Wheel Sizes", "Drum brakes can be packaged more easily within smaller wheel sizes compared to disc brakes, supporting compact vehicle designs.", ValidationStatus.IMAGINARY, RequirementClassification.DERIVED_FROM_PRODUCT_TEST_CASE, "Drum Brake");
      case "5.6": return veReq("5.6", "FrictionChangeByPadReplacement", "It shall be possible to change the friction conditions by replacing the friction material without requiring major changes to the test environment.", ValidationStatus.REAL, RequirementClassification.DERIVED_FROM_PRODUCT_TEST_CASE, "Brake Friction Modification", "Brake Caliper");
      case "5.7": return veReq("5.7", "Brake ECU Integration", "The validation environment shall support integration of an external brake ECU.", ValidationStatus.REAL, RequirementClassification.UNCLASSIFIED);
      case "6.1": return veReq("6.1", "Wheel Speed Measurement", "The validation environment shall provide accurate wheel speed measurements required for ABS control and validation.", ValidationStatus.IMAGINARY, RequirementClassification.DERIVED_FROM_PRODUCT_TEST_CASE, "Test Bench along with ABS");
      case "6.2": return veReq("6.2", "Sensor Integration and Protection", "Sensors (e.g., force, temperature, rotational speed) shall be placed close to the system under test while being protected against brake dust, heat, and fluids.", ValidationStatus.REAL, RequirementClassification.DERIVED_FROM_TEST_BENCH_ITSELF, "General");
      case "6.3": return veReq("6.3", "Measurement Data Acquisition and Evaluation", "The system shall record recovered energy, braking torque distribution, wheel slip, and stopping distance to evaluate ABS and recuperative braking performance", ValidationStatus.IMAGINARY, RequirementClassification.DERIVED_FROM_PRODUCT_TEST_CASE, "Test Bench with ABS & Recuperative Braking");
      case "6.6": return veReq("6.6", "Vehicle Dynamics Measurement", "The validation environment shall acquire vehicle dynamics signals required for brake system validation (e.g. longitudinal acceleration, yaw rate and steering angle", ValidationStatus.REAL, RequirementClassification.UNCLASSIFIED);
      case "6.7": return veReq("6.7", "Synchronized Signal Acquisition", "The validation environment shall synchronously acquire all relevant measurement signals required for brake system validation.", ValidationStatus.REAL, RequirementClassification.UNCLASSIFIED);
      default:
        throw new IllegalArgumentException("Requirement " + id + " is not part of the V87 catalogue");
    }
  }

  public static Requirement veReq(String id, String name, String text, ValidationStatus status,
      RequirementClassification classification, String... categories) {
    Requirement requirement = Cameo_requirementsFactory.eINSTANCE.createRequirement();
    requirement.setId(id);
    requirement.setName(name);
    requirement.setText(text);
    requirement.setStatus(status);
    requirement.setClassification(classification);
    requirement.getCategories().addAll(List.of(categories));
    return requirement;
  }

  // ===========================================================================
  // Builders and lookups
  // ===========================================================================

  private static Requirement add(RequirementsRepository repository, Requirement requirement, String generation) {
    requirement.getGenerations().add(generation);
    repository.getRequirements().add(requirement);
    return requirement;
  }

  private static void covers(Requirement requirement, Requirement... validationNeeds) {
    requirement.getReferences().addAll(List.of(validationNeeds));
  }

  public static Requirement requirement(RequirementsRepository repository, String id) {
    return repository.getRequirements().stream().filter(r -> id.equals(r.getId())).findFirst()
        .orElseThrow(() -> new IllegalArgumentException("No VE requirement " + id));
  }

  /** Validation need derived by CPRs "1" from the product function with the given name. */
  public static Requirement functionNeed(RequirementsRepository repository, String productFunctionName) {
    return byName(repository, "Validation need: product function '" + productFunctionName + "'");
  }

  /** Validation need derived by CPRs "2" from the product component with the given name. */
  public static Requirement componentNeed(RequirementsRepository repository, String productComponentName) {
    return byName(repository, "Validation need: product component '" + productComponentName + "'");
  }

  /** Aggregated need "extension of the remaining vehicle simulation" of a generation (CPRs "2"). */
  public static Requirement restVehicleNeed(RequirementsRepository repository, String generation) {
    return byName(repository, "Validation need: extension of the remaining vehicle simulation (" + generation + ")");
  }

  private static Requirement byName(RequirementsRepository repository, String name) {
    return repository.getRequirements().stream().filter(r -> name.equals(r.getName())).findFirst()
        .orElseThrow(() -> new IllegalArgumentException("No VE requirement named \"" + name + "\""));
  }

  private static Function function(FunctionsRepository repository, String name, String parentName, String generation,
      RequirementsRepository requirements, String... satisfiedIds) {
    Function function = Cameo_functionsFactory.eINSTANCE.createFunction();
    function.setName(name);
    function.getGenerations().add(generation);
    for (String id : satisfiedIds) {
      function.getSatisfies().add(requirement(requirements, id));
    }
    if (parentName != null) {
      function(repository, parentName).getDependencies().add(function);
    }
    repository.getFunctions().add(function);
    return function;
  }

  private static Function function(FunctionsRepository repository, String name, String parentName, String generation) {
    return function(repository, name, parentName, generation, null);
  }

  private static void satisfies(FunctionsRepository functions, RequirementsRepository requirements, String functionName,
      String... ids) {
    Function function = function(functions, functionName);
    for (String id : ids) {
      function.getSatisfies().add(requirement(requirements, id));
    }
  }

  public static Function function(FunctionsRepository repository, String name) {
    return repository.getFunctions().stream().filter(f -> name.equals(f.getName())).findFirst()
        .orElseThrow(() -> new IllegalArgumentException("No VE function " + name));
  }

  private static ValidationConfiguration configuration(BlockRepository repository, String name, String generation) {
    ValidationConfiguration configuration = Cameo_architectureFactory.eINSTANCE.createValidationConfiguration();
    configuration.setName(name);
    configuration.setGeneration(generation);
    repository.getConfigurations().add(configuration);
    return configuration;
  }

  public static ValidationConfiguration configurationOf(BlockRepository repository, String generation) {
    return repository.getConfigurations().stream().filter(c -> generation.equals(c.getGeneration())).findFirst()
        .orElseThrow(() -> new IllegalArgumentException("No validation configuration for " + generation));
  }

  private static Block block(BlockRepository repository, String name, boolean imaginary) {
    Block block = Cameo_architectureFactory.eINSTANCE.createBlock();
    block.setName(name);
    block.setImaginary(imaginary);
    repository.getBlocks().add(block);
    return block;
  }

  public static Block block(BlockRepository repository, String name) {
    return repository.getBlocks().stream().filter(b -> name.equals(b.getName())).findFirst()
        .orElseThrow(() -> new IllegalArgumentException("No VE block " + name));
  }

  private static void replace(ValidationConfiguration configuration, BlockRepository repository, String oldName,
      String newName) {
    configuration.getParts().remove(block(repository, oldName));
    configuration.getParts().add(block(repository, newName, true));
  }

  private static void allocate(BlockRepository architecture, FunctionsRepository functions, String blockName,
      String... functionNames) {
    Block block = block(architecture, blockName);
    for (String functionName : functionNames) {
      block.getAllocatedFunctions().add(function(functions, functionName));
    }
  }
}
