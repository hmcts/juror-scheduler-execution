package uk.gov.hmcts.juror.job.execution.jobs.dashboard.ams;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.juror.job.execution.config.DatabaseConfig;
import uk.gov.hmcts.juror.job.execution.config.contracts.HasDatabaseConfig;

import java.io.File;

@Configuration
@ConfigurationProperties(prefix = "jobs.dashboard.ams")
@Getter
@Setter
public class AmsDashboardConfig implements HasDatabaseConfig {


    @NestedConfigurationProperty
    private DatabaseConfig database;

    @NotNull
    private File pncCertificateLocation;

    @NotNull
    private File dashboardCsvLocation;

    @NotBlank
    private String pncCertificatePassword;

    @NotBlank
    private String pncCertificateAlias;
}
