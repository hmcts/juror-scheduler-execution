package uk.gov.hmcts.juror.job.execution.jobs.contentstore.print;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.juror.job.execution.config.DatabaseConfig;
import uk.gov.hmcts.juror.job.execution.config.SftpConfig;
import uk.gov.hmcts.juror.job.execution.config.contracts.HasDatabaseConfig;
import uk.gov.hmcts.juror.job.execution.config.contracts.HasSftpConfig;

import java.io.File;

@Configuration
@ConfigurationProperties(prefix = "jobs.print")
@Getter
@Setter
public class PrintConfig implements HasDatabaseConfig, HasSftpConfig {
    @NestedConfigurationProperty
    private SftpConfig sftp;

    @NestedConfigurationProperty
    private DatabaseConfig database;
    @NotNull
    private Integer printFileRowLimit;
    @NotNull
    private File ftpDirectory;

    private long retryLimit;
    private long retryDelay;
}
