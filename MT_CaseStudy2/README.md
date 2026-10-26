# Replication Package for Integration of Model-Based Activities for Supporting the Synthesis of Validation Environments in the V-SUM Approach

The scenarios are implemented as Unit test cases and can be easily built using the provided Maven projects. 
Simply navigate to the project's root directory and execute `mvn clean verify` to build the project and execute the tests.

The scenarios from the paper can be found in the files:
1. `vsum/src/test/java/tools/vitruv/methodologisttemplate/vsum/Scenario1CameoBlockToSimulinkSubsystemConsistencyTest.java`
2. `vsum/src/test/java/tools/vitruv/methodologisttemplate/vsum/Scenario2SimulinkSubsystemToCameoConsistencyTest.java`

For any questions, please contact all three authors of the replication package:
patrick.grycz@kit.edu, nathan.hagel@kit.edu, fabian.eger@kit.edu

## Case Study 2: Brake-system generations (Gn-1 -> Gn -> Gn+1)

Based on `Convide_C01S_CaseStudy2_261020.pptx` and the Cameo exports `Bremssystem_Gn-1.xml`, `Bremssystem_Gn.xml`,
`Bremssystem_Gn+1_K1.xml` and `Brake_system-in-the-Loop_Validierung_V87_PG.xml`.

**Two views (slide 9).**
- Product developer: new metamodels `product_requirements.ecore`, `product_functions.ecore`, `product_architecture.ecore`
  (`model/src/main/ecore`). One set of models per generation; each repository knows its `generation` and its `predecessor`.
- Validation engineer: the existing `cameo_requirements/functions/architecture.ecore`, extended (additive only, Case Study 1 still works)
  by the V87 tags `generations`, `status` (Real/Imaginary), `classification`, `categories`, plus `reviewRequired`,
  `restVehicleSimulationAffected`, VE function `satisfies`, block `allocatedFunctions`/`imaginary` and one
  `ValidationConfiguration` per generation.

**CPRs (`consistency/src/main/reactions/.../consistency`).**
| File | Slide 2 | What it does |
|---|---|---|
| `productFunctionsToValidationRequirements.reactions` | CPRs "1" | New product function in generation G -> imaginary *validation need* in the VE requirements, tagged G. Carried-over function (same name + generalisation as in the predecessor) -> linked to the predecessor's need, nothing new. Registering G carries all VE requirements of the predecessor over to G. Rename -> need flagged for review. Removal -> only G is removed from the need's tags. |
| `productArchitectureToValidationRequirements.reactions` | CPRs "2" | Same for product components; identity = name **and** owner. Need text depends on the component kind (sensor, control unit, hydraulics, e-drive, ...). Sensors / operating elements / e-drive additionally feed one need per generation "extension of the remaining vehicle simulation". |
| `productFunctionsToValidationFunctions.reactions` | - | Generation carry-over of the VE functions. |
| `productArchitectureToValidationArchitecture.reactions` | - | Carries the validation configuration of the predecessor over as "Validation Configuration (G)". |

As stated on slide 2 there are no CPRs product requirements -> VE requirements, architecture <-> architecture, or inside one
system model. The Case Study 2 V-SUM (`CaseStudy2Vsum`) therefore registers only these four reaction sets.

**Instances (`vsum/src/main/java/.../vsum`).** `CaseStudy2ProductModels` (product Gn-1 from the Cameo export and the
product steps of slides 3 and 6), `CaseStudy2ValidationEnvironmentModels` (V87 content, assigned to the steps by the red
boxes of slides 5 and 8), `CaseStudy2Scenarios` (one method per stakeholder step), `ValidationNeedCoverage`
(which needs of a generation are not yet covered by a VE requirement) and `CaseStudy2Example` (runs everything, prints the
coverage, persists to `casestudy2-vsum/`).

**Tests (`vsum/src/test/java/.../vsum`).**
- `CaseStudy2Scenario1AbsIntroductionTest` - slide 3, steps 1-21, starting from the Gn-1 state.
- `CaseStudy2Scenario2RecuperativeBrakingTest` - slide 6, steps 6-10, starting from the state after scenario 1.
- `CaseStudy2GenerationScopedChangesTest` - deletion / rename in one generation, identity by owner.

**Initial states (snapshot 0).** Every scenario test loads exactly one V-SUM snapshot - the state before the scenario
(product and test-bench models plus the correspondence model) - from `vsum/src/test/resources/casestudy2/snapshots/`, and then
performs all steps in code. No further snapshots are loaded.
- `scenario1-initial-state-gn-1`: product Gn-1 and the validation environment for Gn-1 (used by scenario 1).
- `scenario2-initial-state-gn`: product Gn-1 and Gn and the validation environment after scenario 1 (used by scenario 2 and
  `CaseStudy2GenerationScopedChangesTest`).

The snapshots are generated from `CaseStudy2Scenarios` by `CaseStudy2Snapshots.main` (run it in the `vsum` module after
changing metamodels, reactions or instances). The final V-SUM state of each test stays under `vsum/target/casestudy2-*`.
