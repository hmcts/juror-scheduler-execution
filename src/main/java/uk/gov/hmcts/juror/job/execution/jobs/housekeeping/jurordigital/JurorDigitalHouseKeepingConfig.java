package uk.gov.hmcts.juror.job.execution.jobs.housekeeping.jurordigital;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.juror.job.execution.config.DatabaseConfig;
import uk.gov.hmcts.juror.job.execution.config.contracts.HasDatabaseConfig;

@Configuration
@ConfigurationProperties(prefix = "jobs.housekeeping.juror-digital")
@Getter
@Setter
public class JurorDigitalHouseKeepingConfig implements HasDatabaseConfig {

    @NestedConfigurationProperty
    private DatabaseConfig database;

    @NotNull
    private int retentionThreshold;
}
