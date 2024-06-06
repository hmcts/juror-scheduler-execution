package uk.gov.hmcts.juror.job.execution.jobs.stats;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.juror.job.execution.config.DatabaseConfig;

@Configuration
@ConfigurationProperties(prefix = "jobs.stats.performance")
@Getter
@Setter
public class PerformanceStatsConfig {

    @NestedConfigurationProperty
    private DatabaseConfig database;

    @NotNull
    private Integer responseTimesAndNonRespondNoMonths;
    @NotNull
    private Integer welshOnlineResponsesNoMonths;
    @NotNull
    private Integer thirdpartyOnlineNoMonths;
    @NotNull
    private Integer deferralsNoMonths;
    @NotNull
    private Integer excusalsNoMonths;
}
