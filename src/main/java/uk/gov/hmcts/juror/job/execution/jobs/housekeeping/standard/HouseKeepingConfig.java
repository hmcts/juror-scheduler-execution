package uk.gov.hmcts.juror.job.execution.jobs.housekeeping.standard;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.juror.job.execution.config.DatabaseConfig;
import uk.gov.hmcts.juror.job.execution.config.contracts.HasDatabaseConfig;

@Configuration
@ConfigurationProperties(prefix = "jobs.housekeeping.standard")
@Getter
@Setter
public class HouseKeepingConfig implements HasDatabaseConfig {

    @NestedConfigurationProperty
    private DatabaseConfig database;

    @NotNull
    private Boolean ownerRestrict;
    @NotNull
    private Boolean readOnly;

    @NotNull
    @Min(600)
    private int maxTimeout;
}
