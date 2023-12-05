package uk.gov.hmcts.juror.job.execution.jobs.checks.pnc.batch;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.juror.job.execution.config.DatabaseConfig;

@Configuration
@ConfigurationProperties(prefix = "jobs.checks.pnc.batch")
@Getter
@Setter
public class PncBatchConfig {
    @NestedConfigurationProperty
    private DatabaseConfig database;

    @NotNull
    private Integer batchSize;
}