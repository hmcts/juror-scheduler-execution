package uk.gov.hmcts.juror.job.execution.testsupport;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.sshd.sftp.client.SftpClient;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.expression.Expression;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.file.remote.RemoteFileTemplate;
import org.springframework.integration.file.remote.handler.FileTransferringMessageHandler;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.handler.ExpressionEvaluatingMessageProcessor;
import org.springframework.integration.sftp.outbound.SftpMessageHandler;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;
import org.springframework.integration.sftp.session.SftpRemoteFileTemplate;
import org.springframework.integration.util.SimplePool;
import org.springframework.messaging.MessageHandler;
import uk.gov.hmcts.juror.job.execution.config.SftpConfig;
import uk.gov.hmcts.juror.job.execution.config.contracts.HasSftpConfig;
import uk.gov.hmcts.juror.job.execution.util.Sftp;

import java.io.File;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

@SuppressWarnings("unchecked")
public abstract class AbstractSftpTest<S extends Sftp, C extends HasSftpConfig, G extends Sftp.SftpServerGateway> {

    protected abstract S createSftp(C config);

    protected abstract C createConfig();

    protected abstract String getName();

    protected abstract Class<S> getSftpClass();

    protected abstract Class<G> getSftpServerGatewayClass();

    @Test
    protected void verifySftpSessionFactoryBeanAnnotation() throws Exception {
        Method method = getSftpClass().getMethod("sftpSessionFactory");
        assertTrue(method.isAnnotationPresent(Bean.class), "sftpSessionFactory method should be annotated with @Bean");
        Bean bean = method.getAnnotation(Bean.class);
        assertEquals(0, bean.value().length, "sftpSessionFactory method should be annotated with @Bean with no name");
        assertEquals(getName() + "SftpSessionFactory", bean.name()[0],
            "sftpSessionFactory method should be annotated with @Bean with name " + getName() + "SftpSessionFactory");
    }

    @Test
    protected void verifyToSftpChannelPrintDestinationHandlerBeanAnnotation() throws Exception {
        Method method = getSftpClass().getMethod("toSftpChannelPrintDestinationHandler");
        assertTrue(method.isAnnotationPresent(Bean.class),
            "toSftpChannelPrintDestinationHandler method should be annotated with @Bean");
        Bean bean = method.getAnnotation(Bean.class);
        assertEquals(0, bean.value().length,
            "toSftpChannelPrintDestinationHandler method should be annotated with @Bean with no name");
        assertEquals(getName() + "ToSftpChannelPrintDestinationHandler", bean.name()[0],
            "toSftpChannelPrintDestinationHandler method should be annotated with @Bean with name " + getName()
                + "ToSftpChannelPrintDestinationHandler");
    }

    @Test
    protected void verifyToSftpChannelPrintDestinationHandlerServiceActivatorAnnotation() throws Exception {
        Method method = getSftpClass().getMethod("toSftpChannelPrintDestinationHandler");
        assertTrue(method.isAnnotationPresent(Bean.class),
            "toSftpChannelPrintDestinationHandler method should be annotated with @Bean");
        assertTrue(method.isAnnotationPresent(ServiceActivator.class),
            "toSftpChannelPrintDestinationHandler method should be annotated with @ServiceActivator");
        ServiceActivator serviceActivator = method.getAnnotation(ServiceActivator.class);

        assertEquals("toSftpChannel" + getName() + "Destination", serviceActivator.inputChannel(),
            "toSftpChannelPrintDestinationHandler method should be annotated with @ServiceActivator with inputChannel"
                + " toSftpChannel"
                + getName() + "Destination");
    }

    @Test
    protected void verifyPrintSftpServerGatewayImplGetParent() {
        G gateway = spy(getSftpServerGatewayClass());
        assertEquals(getSftpClass(), gateway.getParent(),
            "SftpServerGatewayImpl should return the correct parent class");
        assertEquals(getSftpClass(), getSftpServerGatewayClass().getEnclosingClass(),
            "SftpServerGatewayImpl should be an inner class of the correct parent class");
    }

    @Test
    protected void verifyPrintSftpServerGatewayImplAnnotations() throws Exception {
        Method method = getSftpServerGatewayClass().getMethod("upload", File.class);
        assertTrue(method.isAnnotationPresent(Gateway.class),
            "upload method should be annotated with @Gateway");
        Gateway gateway = method.getAnnotation(Gateway.class);
        assertEquals("toSftpChannel" + getName() + "Destination", gateway.requestChannel(),
            "upload method should be annotated with @Gateway with requestChannel"
                + " toSftpChannel" + getName() + "Destination");

    }

    @Test
    protected void constructorTest() {
        SftpConfig sftpConfig = mock(SftpConfig.class);
        C config = createConfig();
        config.setSftp(sftpConfig);
        S sftp = createSftp(config);
        assertSame(sftpConfig, sftp.getConfig(),
            "Sftp config should be set in the constructor");
    }


    @Test
    protected void positiveSftpSessionFactoryTypical() {
        SftpConfig sftpConfig = createSftpConfig(false);
        C config = createConfig();
        config.setSftp(sftpConfig);
        S sftp = createSftp(config);

        SessionFactory<SftpClient.DirEntry> sessionFactory = sftp.sftpSessionFactory();
        validateSessionFactory(sftpConfig, sessionFactory);
    }


    @Test
    protected void positiveSftpSessionFactoryAzure() {
        SftpConfig sftpConfig = createSftpConfig(true);
        C config = createConfig();
        config.setSftp(sftpConfig);
        S sftp = createSftp(config);

        SessionFactory<SftpClient.DirEntry> sessionFactory = sftp.sftpSessionFactory();
        validateSessionFactory(sftpConfig, sessionFactory);
    }

    @Test
    protected void positiveToSftpChannelPrintDestinationHandler() {
        SftpConfig sftpConfig = createSftpConfig(false);
        C config = createConfig();
        config.setSftp(sftpConfig);
        S sftp = createSftp(config);

        MessageHandler messageHandler = sftp.toSftpChannelPrintDestinationHandler();

        assertInstanceOf(SftpMessageHandler.class, messageHandler,
            "Message handler is not of type SftpMessageHandler");
        SftpMessageHandler sftpMessageHandler = (SftpMessageHandler) messageHandler;


        SftpRemoteFileTemplate remoteFileTemplate = TestUtil.getFieldValue(SftpRemoteFileTemplate.class,
            FileTransferringMessageHandler.class,
            "remoteFileTemplate", sftpMessageHandler);

        validateSessionFactory(sftpConfig, remoteFileTemplate.getSessionFactory());
        assertTrue(sftpMessageHandler.isLoggingEnabled(),
            "Logging enabled is not set correctly");


        ExpressionEvaluatingMessageProcessor<String> directoryExpressionProcessor = TestUtil.getFieldValue(
            ExpressionEvaluatingMessageProcessor.class,
            RemoteFileTemplate.class, "directoryExpressionProcessor", remoteFileTemplate);

        Expression expression = TestUtil.getFieldValue(Expression.class, "expression", directoryExpressionProcessor);

        assertEquals("", expression.getExpressionString(),
            "Remote directory is not set correctly");

    }

    protected void validateSessionFactory(SftpConfig config, SessionFactory<SftpClient.DirEntry> sessionFactory) {

        assertInstanceOf(CachingSessionFactory.class, sessionFactory);
        CachingSessionFactory<SftpClient.DirEntry> cachingSessionFactory =
            (CachingSessionFactory<SftpClient.DirEntry>) sessionFactory;

        SessionFactory<SftpClient.DirEntry> internalSessionFactory =
            TestUtil.getFieldValue(SessionFactory.class, "sessionFactory", cachingSessionFactory);

        assertInstanceOf(DefaultSftpSessionFactory.class, internalSessionFactory,
            "Internal session factory is not of type DefaultSftpSessionFactory");
        DefaultSftpSessionFactory internalDefaultSftpSessionFactory =
            (DefaultSftpSessionFactory) internalSessionFactory;

        assertEquals(config.getHost(),
            TestUtil.getFieldValue(String.class, "host", internalDefaultSftpSessionFactory),
            "Host is not set correctly");
        assertEquals(config.getPort(),
            TestUtil.getFieldValue(Integer.class, "port", internalDefaultSftpSessionFactory),
            "Port is not set correctly");
        assertEquals(config.getUser(), TestUtil.getFieldValue(String.class, "user",
                internalDefaultSftpSessionFactory),
            "User is not set correctly");

        assertEquals(config.isAllowUnknownKeys(), TestUtil.getFieldValue(Boolean.class, "allowUnknownKeys",
                internalDefaultSftpSessionFactory),
            "Allow unknown keys is not set correctly");

        SimplePool<Session<SftpClient.DirEntry>> pool =
            TestUtil.getFieldValue(SimplePool.class, "pool", cachingSessionFactory);

        assertEquals(config.getPoolSize(), pool.getPoolSize(),
            "Pool size is not set correctly");
        assertEquals(config.getSessionWaitTimeout(), TestUtil.getFieldValue(Long.class, "waitTimeout", pool),
            "Session wait timeout is not set correctly");

        if (config.isAzureDeployment()) {
            assertNull(TestUtil.getFieldValue(String.class, "password", internalDefaultSftpSessionFactory),
                "Password is not set correctly");
            assertEquals(config.getPrivateKey(),
                TestUtil.getFieldValue(Resource.class, "privateKey", internalDefaultSftpSessionFactory).getFilename(),
                "Private key is not set correctly");
            assertEquals(config.getPrivateKeyPassPhrase(),
                TestUtil.getFieldValue(String.class, "privateKeyPassphrase", internalDefaultSftpSessionFactory),
                "Private key passphrase is not set correctly");
        } else {
            assertNull(TestUtil.getFieldValue(String.class, "privateKey", internalDefaultSftpSessionFactory),
                "Private key is not set correctly");
            assertNull(TestUtil.getFieldValue(String.class, "privateKeyPassphrase", internalDefaultSftpSessionFactory),
                "Private key passphrase is not set correctly");
            assertEquals(config.getPassword(),
                TestUtil.getFieldValue(String.class, "password", internalDefaultSftpSessionFactory),
                "Password is not set correctly");

        }
    }


    protected SftpConfig createSftpConfig(boolean isAzure) {
        SftpConfig sftpConfig = new SftpConfig();

        sftpConfig.setHost(RandomStringUtils.randomAlphabetic(10));
        sftpConfig.setPort(RandomUtils.nextInt());
        sftpConfig.setUser(RandomStringUtils.randomAlphabetic(10));
        sftpConfig.setAllowUnknownKeys(RandomUtils.nextBoolean());
        sftpConfig.setPoolSize(RandomUtils.nextInt());
        sftpConfig.setSessionWaitTimeout(RandomUtils.nextInt());


        if (isAzure) {
            sftpConfig.setAzureDeployment(true);
            sftpConfig.setPrivateKey(RandomStringUtils.randomAlphabetic(10));
            sftpConfig.setPrivateKeyPassPhrase(RandomStringUtils.randomAlphabetic(10));
        } else {
            sftpConfig.setPassword(RandomStringUtils.randomAlphabetic(10));
        }

        return sftpConfig;
    }
}
