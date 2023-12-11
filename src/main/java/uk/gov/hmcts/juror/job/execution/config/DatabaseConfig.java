package uk.gov.hmcts.juror.job.execution.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "defaults.database")
@Getter
@Setter
public class DatabaseConfig {
    private String username;
    private String password;
    private String url;
    private String schema;
}
