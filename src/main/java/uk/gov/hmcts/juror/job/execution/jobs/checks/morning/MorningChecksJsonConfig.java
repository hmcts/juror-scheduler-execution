package uk.gov.hmcts.juror.job.execution.jobs.checks.morning;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class MorningChecksJsonConfig {

    Map<DayOfWeek, DayConfig> days;

    @Getter
    @Setter
    public static class DayConfig {

        @JsonProperty("expect_jobs")
        private List<String> expectedJobs;

        public List<String> getExpectedJobs() {
            return this.expectedJobs;
        }
    }
}
