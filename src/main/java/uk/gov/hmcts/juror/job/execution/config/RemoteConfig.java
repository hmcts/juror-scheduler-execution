package uk.gov.hmcts.juror.job.execution.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.juror.standard.config.WebConfig;

@ConfigurationProperties("uk.gov.hmcts.juror.job.execution.remote")
@Data
@Configuration
public class RemoteConfig {

    @NotNull
    @NestedConfigurationProperty
    private WebConfig schedulerService;

    @NotNull
    @NestedConfigurationProperty
    private WebConfig jurorService;

    @NotBlank
    private String schedulerServiceUpdateStatusUrl;
    @NotBlank
    private String schedulerServiceGetLatestStatusUrl;
    @NotBlank
    private String schedulerServiceGetStatusUrl;
    @NotBlank
    private String schedulerServiceGetTaskSearchUrl;

    @NotNull
    @NestedConfigurationProperty
    private WebConfig policeNationalComputerCheckService;
}
