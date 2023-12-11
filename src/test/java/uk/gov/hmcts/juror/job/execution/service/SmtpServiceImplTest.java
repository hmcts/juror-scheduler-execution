package uk.gov.hmcts.juror.job.execution.service;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import uk.gov.hmcts.juror.job.execution.config.SmtpConfig;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SmtpServiceImplTest {

    private SmtpServiceImpl smtpService;


    @BeforeEach
    void beforeEach() {
        smtpService = new SmtpServiceImpl();
    }

    private SmtpConfig createSmtpConfig() {
        SmtpConfig config = new SmtpConfig();
        config.setHost(RandomStringUtils.randomAlphabetic(10));
        config.setPort(RandomUtils.nextInt());
        config.setUsername(RandomStringUtils.randomAlphabetic(10));
        config.setPassword(RandomStringUtils.randomAlphabetic(10));
        return config;
    }

    @Test
    void positiveGetJavaMailSenderTest() {
        SmtpConfig config = createSmtpConfig();

        JavaMailSender javaMailSender = smtpService.getJavaMailSender(config);

        assertInstanceOf(JavaMailSenderImpl.class, javaMailSender);
        JavaMailSenderImpl javaMailSenderImpl = (JavaMailSenderImpl) javaMailSender;
        assertEquals(config.getHost(), javaMailSenderImpl.getHost(), "Host is not equal");
        assertEquals(config.getPort(), javaMailSenderImpl.getPort(), "Port is not equal");
        assertEquals(config.getUsername(), javaMailSenderImpl.getUsername(), "Username is not equal");
        assertEquals(config.getPassword(), javaMailSenderImpl.getPassword(), "Password is not equal");

        Properties properties = javaMailSenderImpl.getJavaMailProperties();
        assertEquals("smtp", properties.get("mail.transport.protocol"), "Protocol is not equal");
        assertEquals("true", properties.get("mail.smtp.auth"), "Auth is not equal");
        assertEquals("true", properties.get("mail.smtp.starttls.enable"), "Starttls is not equal");
    }

    @Test
    void positiveSendEmailSingleRecipient() {
        final SmtpConfig config = createSmtpConfig();
        final String subject = "This is my very important subject line";
        final String text = "This the body of the email";
        final String recipient = "ben@hmcts.net";
        SmtpServiceImpl smtpService = spy(this.smtpService);
        JavaMailSender javaMailSender = mock(JavaMailSender.class);

        when(smtpService.getJavaMailSender(config)).thenReturn(javaMailSender);
        smtpService.sendEmail(config, subject, text, recipient);


        verify(smtpService, times(1)).getJavaMailSender(config);
        ArgumentCaptor<SimpleMailMessage> simpleMailMessageArgumentCaptor =
            ArgumentCaptor.forClass(SimpleMailMessage.class);

        verify(javaMailSender, times(1)).send(simpleMailMessageArgumentCaptor.capture());

        SimpleMailMessage simpleMailMessage = simpleMailMessageArgumentCaptor.getValue();

        assertEquals(config.getUsername(), simpleMailMessage.getFrom(), "From is not equal");
        assertEquals(subject, simpleMailMessage.getSubject(), "Subject is not equal");
        assertEquals(text, simpleMailMessage.getText(), "Text is not equal");
        assertNotNull(simpleMailMessage.getTo(), "Recipient is null");
        assertEquals(1, simpleMailMessage.getTo().length, "There should be only one recipient");
        assertEquals(recipient, simpleMailMessage.getTo()[0], "Recipient is not equal");
    }

    @Test
    void positiveSendEmailMultipleRecipient() {
        final SmtpConfig config = createSmtpConfig();
        final String subject = "This is my very important subject line";
        final String text = "This the body of the email";
        final String[] recipients = {"ben@hmcts.net", "ben10@hmcts.net", "ben@fishing.net"};
        SmtpServiceImpl smtpService = spy(this.smtpService);
        JavaMailSender javaMailSender = mock(JavaMailSender.class);

        when(smtpService.getJavaMailSender(config)).thenReturn(javaMailSender);
        smtpService.sendEmail(config, subject, text, recipients);


        verify(smtpService, times(1)).getJavaMailSender(config);
        ArgumentCaptor<SimpleMailMessage> simpleMailMessageArgumentCaptor =
            ArgumentCaptor.forClass(SimpleMailMessage.class);

        verify(javaMailSender, times(1)).send(simpleMailMessageArgumentCaptor.capture());

        SimpleMailMessage simpleMailMessage = simpleMailMessageArgumentCaptor.getValue();

        assertEquals(config.getUsername(), simpleMailMessage.getFrom(), "From is not equal");
        assertEquals(subject, simpleMailMessage.getSubject(), "Subject is not equal");
        assertEquals(text, simpleMailMessage.getText(), "Text is not equal");
        assertNotNull(simpleMailMessage.getTo(), "Recipient is null");
        assertEquals(3, simpleMailMessage.getTo().length, "There should be only one recipient");
        assertEquals(recipients, simpleMailMessage.getTo(), "Recipient is not equal");

    }
}
