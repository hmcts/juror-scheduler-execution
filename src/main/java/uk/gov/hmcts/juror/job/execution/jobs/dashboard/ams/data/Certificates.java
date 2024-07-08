package uk.gov.hmcts.juror.job.execution.jobs.dashboard.ams.data;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import uk.gov.hmcts.juror.job.execution.jobs.Job;
import uk.gov.hmcts.juror.job.execution.jobs.dashboard.DashboardDataEntry;
import uk.gov.hmcts.juror.job.execution.jobs.dashboard.ams.AmsDashboardConfig;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Locale;

@Slf4j
@SuppressWarnings("PMD.LawOfDemeter")
public class Certificates extends DashboardDataEntry<DashboardData> {
    private static final DateFormat DATE_FORMATTER =
        new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.getDefault());
    final AmsDashboardConfig config;
    final Clock clock;

    static final String JOB_TITLE = "Certificates";

    public Certificates(DashboardData dashboardData,
                        AmsDashboardConfig config,
                        Clock clock) {
        super(dashboardData, JOB_TITLE, "Name", "Expiry Date", "Status");
        this.config = config;
        this.clock = clock;
    }

    public void addRow(String name, Date expiryDate, String status) {
        if (expiryDate == null) {
            this.addRow(name, "Not Found", status);
        } else {
            this.addRow(name, formatDate(expiryDate), status);
        }
    }

    public void addRow(String name, String expiryDate, String status) {
        this.addEntry(name, expiryDate, status);
    }

    private String formatDate(Date date) {
        synchronized (DATE_FORMATTER) {
            return DATE_FORMATTER.format(date);
        }
    }

    public Job.Result populate() {
        try {
            final String pncCertName = "PNC Certificate";
            String message = null;
            Date expiryDate = null;
            String status = "OK";

            KeyStore keyStore = loadKeyStore(config.getPncCertificateLocation(),
                config.getPncCertificatePassword().toCharArray());

            if (keyStore.containsAlias(config.getPncCertificateAlias())) {
                Certificate certificate = keyStore.getCertificate(config.getPncCertificateAlias());
                if (certificate instanceof X509Certificate x509Certificate) {
                    expiryDate = x509Certificate.getNotAfter();
                } else {
                    message = "Unable to process certificate of type '" + certificate.getType() + "'";
                }
            } else {
                message = "Unable to locate certificate from alias";

            }
            //Expiry date not verified here as this is done via the AMS dashboard
            addRow(
                pncCertName,
                expiryDate,
                status
            );
            populateTimestamp(dashboardData, JOB_TITLE, LocalDateTime.now(clock));
            if (message == null) {
                return Job.Result.passed();
            } else {
                return Job.Result.failed(message);
            }
        } catch (Exception e) {
            populateTimestamp(dashboardData, JOB_TITLE, LocalDateTime.now(clock));
            log.error("Failed to populate certificates", e);
            return Job.Result.failed("Failed to populate certificates. Unexpected exception", e);
        }
    }

    public KeyStore loadKeyStore(final File file, final char... password)
        throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {

        byte[] fileContent = Files.readAllBytes(file.toPath());
        if (this.config.getPncCertificateBase64Encoded()) {
            fileContent = Base64.decodeBase64(fileContent);
        }
        final KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        try (InputStream inputStream = new ByteArrayInputStream(fileContent)) {
            keyStore.load(inputStream, password);
        }
        return keyStore;
    }
}

