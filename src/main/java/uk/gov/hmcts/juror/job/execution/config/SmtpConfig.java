package uk.gov.hmcts.juror.job.execution.config;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "defaults.smtp")
@Getter
@Setter
public class SmtpConfig {

    @Email
    @NotBlank
    private String username;
    @NotBlank
    private String password;

    @NotBlank
    private String host;

    @NotNull
    private Integer port;
}
