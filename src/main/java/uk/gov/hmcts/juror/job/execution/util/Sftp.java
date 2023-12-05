package uk.gov.hmcts.juror.job.execution.util;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.sftp.client.SftpClient;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.sftp.outbound.SftpMessageHandler;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;
import org.springframework.messaging.MessageHandler;
import uk.gov.hmcts.juror.job.execution.config.SftpConfig;

import java.io.File;

@Getter
@Setter
@Slf4j
public abstract class Sftp {

    private final SftpConfig config;

    protected Sftp (SftpConfig config){
        this.config = config;
    }

    public SessionFactory<SftpClient.DirEntry> sftpSessionFactory() {
        DefaultSftpSessionFactory factory = new DefaultSftpSessionFactory(true);
        factory.setHost(this.getConfig().getHost());
        factory.setPort(this.getConfig().getPort());
        factory.setUser(this.getConfig().getUser());

        // Future refactor - after successful migration to Azure, remove the azureDeployment check (if/else statement only).
        if (this.getConfig().isAzureDeployment()) {
            Resource privateKeyResource = new FileSystemResource(this.getConfig().getPrivateKey());
            factory.setPrivateKey(privateKeyResource);
            factory.setPrivateKeyPassphrase(this.getConfig().getPrivateKeyPassPhrase());
        } else {
            factory.setPassword(this.getConfig().getPassword());
        }

        factory.setAllowUnknownKeys(this.getConfig().isAllowUnknownKeys());
        CachingSessionFactory<SftpClient.DirEntry> cachingSessionFactory = new CachingSessionFactory<>(factory);
        cachingSessionFactory.setPoolSize(this.getConfig().getPoolSize());
        cachingSessionFactory.setSessionWaitTimeout(this.getConfig().getSessionWaitTimeout());
        return cachingSessionFactory;
    }

    public MessageHandler toSftpChannelPrintDestinationHandler() {
        SftpMessageHandler handler = new SftpMessageHandler(sftpSessionFactory());
        handler.setRemoteDirectoryExpression(new LiteralExpression(this.getConfig().getRemoteDirectory()));
        handler.setLoggingEnabled(true);
        return handler;
    }

    public interface SftpServerGateway{
        void upload(File file);
        Class<? extends Sftp> getParent();
    }
}
