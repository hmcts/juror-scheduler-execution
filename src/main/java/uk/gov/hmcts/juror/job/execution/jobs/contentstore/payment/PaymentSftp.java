package uk.gov.hmcts.juror.job.execution.jobs.contentstore.payment;

import org.apache.sshd.sftp.client.SftpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.messaging.MessageHandler;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.juror.job.execution.util.Sftp;

import java.io.File;

@Configuration
public class PaymentSftp extends Sftp {
    public static final String NAME = "Payment";

    @Autowired
    protected PaymentSftp(PaymentConfig config) {
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
    public interface PaymentSftpServerGatewayImpl extends SftpServerGateway {
        @Gateway(requestChannel = "toSftpChannel" + NAME + "Destination")
        void upload(File file);

        default Class<? extends Sftp> getParent() {
            return PaymentSftp.class;
        }
    }
}
