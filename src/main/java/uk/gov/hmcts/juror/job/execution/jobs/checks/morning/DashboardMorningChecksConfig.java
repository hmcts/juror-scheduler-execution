package uk.gov.hmcts.juror.job.execution.jobs.checks.morning;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.juror.job.execution.config.SmtpConfig;

import java.io.File;

@Configuration
@ConfigurationProperties(prefix = "jobs.checks.dashboard-morning-checks")
@Getter
@Setter
public class DashboardMorningChecksConfig {

    @NestedConfigurationProperty
    private SmtpConfig smtp;

    @NotNull
    @Email
    @NotBlank
    private String[] emailRecipients;

    @NotNull
    private File archiveFolder;
    @NotNull
    private File attachmentFile;
    @NotNull
    private File checksFile;

    private File expectedJobConfigLocation;
}
