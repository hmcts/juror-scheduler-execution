package uk.gov.hmcts.juror.job.execution.jobs.dashboard.ams;

import org.apache.sshd.sftp.client.SftpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.messaging.MessageHandler;
import uk.gov.hmcts.juror.job.execution.util.Sftp;

import java.io.File;

@Configuration
public class AmsDashboardSftp extends Sftp {
    public static final String NAME = "AmsDashboard";

    @Autowired
    protected AmsDashboardSftp(AmsDashboardConfig config) {
        super(config.getSftp());
    }

    @Bean(name = NAME + "SftpSessionFactory")
    @Override
    public SessionFactory<SftpClient.DirEntry> sftpSessionFactory() {
        return super.sftpSessionFactory();
    }

    @Bean(name = NAME + "ToSftpChannelPrintDestinationHandler")
    @ServiceActivator(inputChannel = "toSftpChannel" + NAME + "Destination")
    @Override
    public MessageHandler toSftpChannelPrintDestinationHandler() {
        return super.toSftpChannelPrintDestinationHandler();
    }

    @MessagingGateway
    public interface AmsDashboardSftpServerGatewayImpl extends SftpServerGateway {
        @Gateway(requestChannel = "toSftpChannel" + NAME + "Destination")
        void upload(File file);

        default Class<? extends Sftp> getParent() {
            return AmsDashboardSftp.class;
        }
    }
}
