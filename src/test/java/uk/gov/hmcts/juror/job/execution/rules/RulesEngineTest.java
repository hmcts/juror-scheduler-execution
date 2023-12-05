package uk.gov.hmcts.juror.job.execution.rules;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RulesEngineTest {

    @Test
    void positiveAllRulesPass() {
        List<Rule> rules = List.of(
            createRule(true, "Rule 1"),
            createRule(true, "Rule 2"),
            createRule(true, "Rule 3")
        );

        List<String> failureMessages = RulesEngine.fire(rules);
        assertEquals(0, failureMessages.size(),
            "All rules should pass as such no failure messages should be returned");
    }

    @Test
    void negativeAllButOneRulesPass() {
        List<Rule> rules = List.of(
            createRule(true, "Rule 1"),
            createRule(false, "Rule 2"),
            createRule(true, "Rule 3")
        );
        List<String> failureMessages = RulesEngine.fire(rules);
        assertEquals(1, failureMessages.size(),
            "All but one rule should pass as such only 1 failure messages should be returned");
        assertEquals("Rule 2", failureMessages.get(0),
            "The failure message should be the one from the failing rule");
    }

    @Test
    void negativeAllRulesFail() {
        List<Rule> rules = List.of(
            createRule(false, "Rule 1"),
            createRule(false, "Rule 2"),
            createRule(false, "Rule 3")
        );
       Set<String> failureMessages = new HashSet<>(RulesEngine.fire(rules));
        assertEquals(3, failureMessages.size(),
            "All rules should fail as such all failure messages should be returned");

        assertTrue(failureMessages.contains("Rule 1"),
            "The failure message should include the one from the first failing rule");
        assertTrue(failureMessages.contains("Rule 2"),
            "The failure message should include the one from the second failing rule");
        assertTrue(failureMessages.contains("Rule 3"),
            "The failure message should include the one from the third failing rule");
    }

    private Rule createRule(boolean result, String message) {
        return new Rule() {
            @Override
            public boolean execute() {
                return result;
            }

            @Override
            public String getMessage() {
                return message;
            }
        };
    }
}
