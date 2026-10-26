package tools.vitruv.methodologisttemplate.vsum;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cameo_requirements.Requirement;
import cameo_requirements.RequirementClassification;
import cameo_requirements.RequirementsRepository;

/**
 * Case Study 2: consistency check on the validation environment.
 *
 * <p>A validation need (derived by CPRs "1" or "2") of generation G is <em>covered</em> if at least one concrete
 * VE requirement tagged with G references it. Uncovered needs show where the validation environment cannot yet
 * validate the product generation.
 */
public final class ValidationNeedCoverage {

  private ValidationNeedCoverage() {
  }

  public static boolean isValidationNeed(Requirement requirement) {
    return requirement.getClassification() == RequirementClassification.VALIDATION_NEED_FROM_PRODUCT_FUNCTION
        || requirement.getClassification() == RequirementClassification.VALIDATION_NEED_FROM_PRODUCT_ARCHITECTURE;
  }

  /** All validation needs that apply to the given generation. */
  public static List<Requirement> validationNeeds(RequirementsRepository repository, String generation) {
    return repository.getRequirements().stream()
        .filter(ValidationNeedCoverage::isValidationNeed)
        .filter(r -> r.getGenerations().contains(generation))
        .toList();
  }

  /** Validation needs of the given generation that no concrete VE requirement of that generation covers. */
  public static List<Requirement> uncoveredValidationNeeds(RequirementsRepository repository, String generation) {
    Set<Requirement> covered = new HashSet<>();
    repository.getRequirements().stream()
        .filter(r -> !isValidationNeed(r))
        .filter(r -> r.getGenerations().contains(generation))
        .forEach(r -> covered.addAll(r.getReferences()));
    return validationNeeds(repository, generation).stream().filter(need -> !covered.contains(need)).toList();
  }

  /** Human readable report, one line per need. */
  public static String report(RequirementsRepository repository, String generation) {
    StringBuilder builder = new StringBuilder("Validation needs of ").append(generation).append(":\n");
    List<Requirement> uncovered = uncoveredValidationNeeds(repository, generation);
    for (Requirement need : validationNeeds(repository, generation)) {
      builder.append(uncovered.contains(need) ? "  [OPEN]    " : "  [covered] ")
          .append(need.getId()).append("  ").append(need.getName())
          .append(need.isReviewRequired() ? "  (review required)" : "").append('\n');
    }
    return builder.toString();
  }
}
