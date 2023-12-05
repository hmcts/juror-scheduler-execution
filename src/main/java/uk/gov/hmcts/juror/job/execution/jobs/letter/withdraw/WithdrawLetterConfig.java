package uk.gov.hmcts.juror.job.execution.jobs.letter.withdraw;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.juror.job.execution.config.DatabaseConfig;
import uk.gov.hmcts.juror.job.execution.config.contracts.HasDatabaseConfig;

@Configuration
@ConfigurationProperties(prefix = "jobs.letter.withdraw")
@Getter
@Setter
public class WithdrawLetterConfig implements HasDatabaseConfig {

    @NestedConfigurationProperty
    private DatabaseConfig database;
}
