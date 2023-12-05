package uk.gov.hmcts.juror.job.execution.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "defaults.sftp")
public class SftpConfig {
    private String host;

    private int port;

    private String user;

    private String remoteDirectory;

    private String privateKey;

    private String privateKeyPassPhrase;

    private boolean allowUnknownKeys;

    private int poolSize;

    private int sessionWaitTimeout;

    private boolean azureDeployment;

    private String password;
}
