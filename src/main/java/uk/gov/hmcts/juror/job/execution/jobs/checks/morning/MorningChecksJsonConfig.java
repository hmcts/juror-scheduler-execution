package uk.gov.hmcts.juror.job.execution.jobs.checks.morning;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class MorningChecksJsonConfig {

    private Map<DayOfWeek, DayConfig> days;

    public DayConfig getDay(DayOfWeek dayOfWeek) {
        return days.get(dayOfWeek);
    }

    @Getter
    @Setter
    public static class DayConfig {

        @JsonProperty("expect_jobs")
        private List<String> expectedJobs;
    }
}
