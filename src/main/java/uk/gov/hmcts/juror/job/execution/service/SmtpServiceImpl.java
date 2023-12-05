package uk.gov.hmcts.juror.job.execution.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.juror.job.execution.config.SmtpConfig;
import uk.gov.hmcts.juror.job.execution.service.contracts.SmtpService;

import java.util.Properties;

@Service
public class SmtpServiceImpl implements SmtpService {

    public JavaMailSender getJavaMailSender(SmtpConfig config) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(config.getHost());
        mailSender.setPort(config.getPort());
        mailSender.setUsername(config.getUsername());
        mailSender.setPassword(config.getPassword());
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        return mailSender;
    }

    @Override
    public void sendEmail(SmtpConfig config, String subject, String text, String... recipients) {
        JavaMailSender javaMailSender = getJavaMailSender(config);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(config.getUsername());
        message.setTo(recipients);
        message.setSubject(subject);
        message.setText(text);

        javaMailSender.send(message);
    }
}
