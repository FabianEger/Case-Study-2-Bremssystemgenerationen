package tools.vitruv.methodologisttemplate.vsum;

import static tools.vitruv.methodologisttemplate.vsum.CaseStudy2Vsum.GN;
import static tools.vitruv.methodologisttemplate.vsum.CaseStudy2Vsum.GN_MINUS_1;
import static tools.vitruv.methodologisttemplate.vsum.CaseStudy2Vsum.GN_PLUS_1;
import static tools.vitruv.methodologisttemplate.vsum.CaseStudy2Vsum.VALIDATION_ENVIRONMENT_ROOTS;

import java.nio.file.Path;

import cameo_requirements.RequirementsRepository;
import tools.vitruv.framework.vsum.VirtualModel;

/**
 * Case Study 2 example (counterpart of {@link VSUMExample}): builds the V-SUM in its Gn-1 state, runs
 * scenario 1 (Gn-1 -&gt; Gn, ABS) and scenario 2 (Gn -&gt; Gn+1, recuperative braking) and prints the
 * coverage of the validation needs per generation. The models are persisted in {@code casestudy2-vsum/}.
 */
public class CaseStudy2Example {

  public static void main(String[] args) {
    Path storage = Path.of(args.length > 0 ? args[0] : "casestudy2-vsum").toAbsolutePath();
    VirtualModel vsum = CaseStudy2Vsum.createVirtualModel(storage);

    CaseStudy2Scenarios.initialiseGnMinus1(vsum, storage);
    report(vsum, "Initial state", GN_MINUS_1);

    CaseStudy2Scenarios.runScenario1(vsum, storage);
    report(vsum, "After scenario 1 (ABS)", GN);

    CaseStudy2Scenarios.runScenario2(vsum, storage);
    report(vsum, "After scenario 2 (recuperative braking)", GN_PLUS_1);

    System.out.println("Models written to " + storage);
  }

  private static void report(VirtualModel vsum, String title, String generation) {
    RequirementsRepository requirements = CaseStudy2Vsum.veRequirements(
        CaseStudy2Vsum.view(vsum, VALIDATION_ENVIRONMENT_ROOTS));
    System.out.println("=== " + title);
    System.out.println(ValidationNeedCoverage.report(requirements, generation));
  }
}
