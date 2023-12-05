package uk.gov.hmcts.juror.job.execution.jobs.letter.confirm;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.juror.job.execution.config.DatabaseConfig;
import uk.gov.hmcts.juror.job.execution.config.contracts.HasDatabaseConfig;

@Configuration
@ConfigurationProperties(prefix = "jobs.letter.confirm")
@Getter
@Setter
public class ConfirmLetterConfig implements HasDatabaseConfig {

    @NestedConfigurationProperty
    private DatabaseConfig database;
}
