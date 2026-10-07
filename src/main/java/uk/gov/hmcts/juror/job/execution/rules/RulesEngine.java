package uk.gov.hmcts.juror.job.execution.rules;

import jakarta.validation.constraints.NotNull;

import java.util.Collection;
import java.util.List;


public final class RulesEngine {
    private RulesEngine() {

    }

    public static List<String> fire(@NotNull Collection<Rule> rules) {
        return rules.parallelStream()
            .filter(rule -> !rule.execute())
            .map(Rule::getMessage)
            .toList();
    }
}
