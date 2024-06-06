package uk.gov.hmcts.juror.job.execution.jobs.checks.morning;


import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.io.File;

@Configuration
@ConfigurationProperties(prefix = "jobs.checks.dashboard-morning-checks")
@Getter
@Setter
public class DashboardMorningChecksConfig {
    @NotNull
    private File archiveFolder;
    @NotNull
    private File attachmentFile;
    @NotNull
    private File checksFile;

    private File expectedJobConfigLocation;
}
