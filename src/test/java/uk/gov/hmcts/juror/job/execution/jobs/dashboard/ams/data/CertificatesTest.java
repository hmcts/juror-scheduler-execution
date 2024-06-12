package uk.gov.hmcts.juror.job.execution.jobs.dashboard.ams.data;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import uk.gov.hmcts.juror.job.execution.jobs.Job;
import uk.gov.hmcts.juror.job.execution.jobs.dashboard.ams.AmsDashboardConfig;
import uk.gov.hmcts.juror.job.execution.jobs.dashboard.ams.AmsDashboardGenerateJobTest;

import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CertificatesTest {

    private AmsDashboardConfig config;
    private Clock clock;
    private DashboardData dashboardData;
    private Certificates certificates;

    @BeforeEach
    void beforeEach() {
        this.config = AmsDashboardGenerateJobTest.createConfig();
        this.clock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
        this.dashboardData = mock(DashboardData.class);
        this.certificates = spy(new Certificates(dashboardData, config, clock));

    }

    @Test
    void positiveConstructorTest() {
        assertSame(dashboardData, certificates.dashboardData,
            "Expected databaseService to be set");
        assertSame(config, certificates.config, "Expected databaseConfig to be set");
        assertSame(clock, certificates.clock, "Expected clock to be set");

        assertEquals("Certificates", certificates.title,
            "Expected title to be Certificates");
        assertEquals(3, certificates.columCount,
            "Expected column count to be 3");
        assertEquals(3, certificates.rows.get(0).length);
        assertEquals("Name", certificates.rows.get(0)[0]);
        assertEquals("Expiry Date", certificates.rows.get(0)[1]);
        assertEquals("Status", certificates.rows.get(0)[2]);
    }

    @Test
    void positiveAddRowDateTest() {
        doNothing().when(certificates).addEntry(any(), any(), any());
        Date date = new Date();
        certificates.addRow("testName", date, "testStatus");

        verify(certificates, times(1)).addEntry("testName",
            new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy").format(date),
            "testStatus");
    }

    @Test
    void positiveAddRowDateWithoutExpiryDateTest() {
        doNothing().when(certificates).addEntry(any(), any(), any());
        certificates.addRow("testName", (Date) null, "testStatus");

        verify(certificates, times(1)).addEntry("testName",
            "Not Found",
            "testStatus");
    }

    @Test
    void positiveAddRowStringTest() {
        doNothing().when(certificates).addEntry(any(), any(), any());
        certificates.addRow("testName", "testDate", "testStatus");

        verify(certificates, times(1)).addEntry("testName",
            "testDate",
            "testStatus");
    }

    @Test
    void positivePopulateCertificateDoesNotExpiredWithin30Days() throws Exception {

        KeyStore keyStore = mock(KeyStore.class);
        X509Certificate certificate = mock(X509Certificate.class);

        Date expiryDate = Date.from(clock.instant().minus(31, ChronoUnit.DAYS));
        doReturn(expiryDate).when(certificate).getNotAfter();

        doReturn(keyStore).when(certificates).loadKeyStore(config.getPncCertificateLocation(),
            config.getPncCertificatePassword().toCharArray());

        doNothing().when(certificates).addRow(any(), any(Date.class), any());
        doNothing().when(certificates).populateTimestamp(any(), any(), any(LocalDateTime.class));

        doReturn(true).when(keyStore).containsAlias(config.getPncCertificateAlias());
        doReturn(certificate).when(keyStore).getCertificate(config.getPncCertificateAlias());


        Job.Result result = certificates.populate();

        verify(certificates, times(1))
            .addRow("PNC Certificate", expiryDate, "OK");
        verify(certificates, times(1))
            .populateTimestamp(dashboardData, "Certificates", LocalDateTime.now(clock));


        assertEquals(Job.Result.passed(), result, "Result should be passed");

        Mockito.verify(certificates, times(1)).loadKeyStore(config.getPncCertificateLocation(),
            config.getPncCertificatePassword().toCharArray());
    }

    @Test
    void negativePopulateCanNotFindCertificateAlias() throws Exception {

        KeyStore keyStore = mock(KeyStore.class);
        doReturn(keyStore).when(certificates).loadKeyStore(config.getPncCertificateLocation(),
            config.getPncCertificatePassword().toCharArray());

        doNothing().when(certificates).addRow(any(), any(Date.class), any());
        doNothing().when(certificates).populateTimestamp(any(), any(), any(LocalDateTime.class));

        doReturn(false).when(keyStore).containsAlias(config.getPncCertificateAlias());

        Job.Result result = certificates.populate();

        verify(certificates, times(1))
            .addRow("PNC Certificate", "Not Found", "OK");
        verify(certificates, times(1))
            .populateTimestamp(dashboardData, "Certificates", LocalDateTime.now(clock));


        assertEquals(Job.Result.failed("Unable to locate certificate from alias"), result,
            "Result should be failed with message");

        Mockito.verify(certificates, times(1)).loadKeyStore(config.getPncCertificateLocation(),
            config.getPncCertificatePassword().toCharArray());
    }

    @Test
    void negativePopulateWrongCertificateType() throws Exception {

        KeyStore keyStore = mock(KeyStore.class);
        Certificate certificate = mock(Certificate.class);
        when(certificate.getType()).thenReturn("SomeCertificateType");

        doReturn(keyStore).when(certificates).loadKeyStore(config.getPncCertificateLocation(),
            config.getPncCertificatePassword().toCharArray());

        doNothing().when(certificates).addRow(any(), any(Date.class), any());
        doNothing().when(certificates).populateTimestamp(any(), any(), any(LocalDateTime.class));

        doReturn(true).when(keyStore).containsAlias(config.getPncCertificateAlias());
        doReturn(certificate).when(keyStore).getCertificate(config.getPncCertificateAlias());

        Job.Result result = certificates.populate();

        verify(certificates, times(1))
            .addRow("PNC Certificate", "Not Found", "OK");
        verify(certificates, times(1))
            .populateTimestamp(dashboardData, "Certificates", LocalDateTime.now(clock));


        assertEquals(Job.Result.failed("Unable to process certificate of type 'SomeCertificateType'"), result,
            "Result should be passed");

        Mockito.verify(certificates, times(1)).loadKeyStore(config.getPncCertificateLocation(),
            config.getPncCertificatePassword().toCharArray());
    }

    @Test
    void negativePopulateCertificateExpiresWithin30Days() throws Exception {

        KeyStore keyStore = mock(KeyStore.class);
        X509Certificate certificate = mock(X509Certificate.class);

        Date expiryDate = Date.from(clock.instant().minus(29, ChronoUnit.DAYS));
        doReturn(expiryDate).when(certificate).getNotAfter();

        doReturn(keyStore).when(certificates).loadKeyStore(config.getPncCertificateLocation(),
            config.getPncCertificatePassword().toCharArray());


        doNothing().when(certificates).addRow(any(), any(Date.class), any());
        doNothing().when(certificates).populateTimestamp(any(), any(), any(LocalDateTime.class));

        doReturn(true).when(keyStore).containsAlias(config.getPncCertificateAlias());
        doReturn(certificate).when(keyStore).getCertificate(config.getPncCertificateAlias());

        Job.Result result = certificates.populate();

        verify(certificates, times(1))
            .addRow("PNC Certificate", expiryDate, "OK");
        verify(certificates, times(1))
            .populateTimestamp(dashboardData, "Certificates", LocalDateTime.now(clock));


        assertEquals(Job.Result.passed(), result, "Result should be passed");

        Mockito.verify(certificates, times(1)).loadKeyStore(config.getPncCertificateLocation(),
            config.getPncCertificatePassword().toCharArray());

    }

    @Test
    void negativePopulateUnexpectedException() throws Exception {

        KeyStore keyStore = mock(KeyStore.class);
        X509Certificate certificate = mock(X509Certificate.class);

        doReturn(keyStore).when(certificates).loadKeyStore(config.getPncCertificateLocation(),
            config.getPncCertificatePassword().toCharArray());


        doNothing().when(certificates).addRow(any(), any(Date.class), any());
        doNothing().when(certificates).populateTimestamp(any(), any(), any(LocalDateTime.class));

        RuntimeException cause = new RuntimeException("I am the cause");
        doThrow(cause).when(keyStore).containsAlias(config.getPncCertificateAlias());
        doReturn(certificate).when(keyStore).getCertificate(config.getPncCertificateAlias());

        Job.Result result = certificates.populate();

        verify(certificates, times(1))
            .populateTimestamp(dashboardData, "Certificates", LocalDateTime.now(clock));

        assertEquals(Job.Result.failed("Failed to populate certificates. Unexpected exception", cause), result,
            "Result should be failed with message");

        Mockito.verify(certificates, times(1)).loadKeyStore(config.getPncCertificateLocation(),
            config.getPncCertificatePassword().toCharArray());
    }
}
