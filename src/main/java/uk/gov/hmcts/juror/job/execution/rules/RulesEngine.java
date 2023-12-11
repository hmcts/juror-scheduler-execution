package uk.gov.hmcts.juror.job.execution.rules;

import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


public final class RulesEngine {
    private RulesEngine() {

    }

    public static List<String> fire(@NotNull Collection<Rule> rules) {
        List<String> failureMessages = new ArrayList<>();
        rules.parallelStream().forEach(rule -> {
            if (!rule.execute()) {
                failureMessages.add(rule.getMessage());
            }
        });
        return failureMessages;
    }
}
