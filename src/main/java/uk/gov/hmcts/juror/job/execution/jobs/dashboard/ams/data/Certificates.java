package uk.gov.hmcts.juror.job.execution.jobs.dashboard.ams.data;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import uk.gov.hmcts.juror.job.execution.jobs.Job;
import uk.gov.hmcts.juror.job.execution.jobs.dashboard.ams.AmsDashboardConfig;
import uk.gov.hmcts.juror.job.execution.service.contracts.SmtpService;

import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Locale;


@Slf4j
@SuppressWarnings("PMD.LawOfDemeter")
public class Certificates extends DashboardDataEntry {
    private static final DateFormat DATE_FORMATTER =
        new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.getDefault());
    final AmsDashboardConfig config;
    final Clock clock;
    final SmtpService smtpService;

    public Certificates(DashboardData dashboardData,
                        AmsDashboardConfig config,
                        SmtpService smtpService,
                        Clock clock) {
        super(dashboardData, "Certificates", "Name", "Expiry Date", "Status");
        this.config = config;
        this.smtpService = smtpService;
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
            KeyStore keyStore = KeyStore
                .getInstance(config.getPncCertificateLocation(),
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
            if (expiryDate != null && expiryDate.after(DateUtils.addDays(Date.from(clock.instant()), -30))
                && config.getSmtp() != null) {
                smtpService.sendEmail(config.getSmtp(),
                    "PNC Certificate Expires soon",
                    "The PNC Certificate is due to expire at " + expiryDate + ". Please update the certificate.",
                    config.getEmailRecipients());
            }
            populateTimestamp(dashboardData, "Certificates", LocalDateTime.now(clock));
            if (message == null) {
                return Job.Result.passed();
            } else {
                return Job.Result.failed(message);
            }
        } catch (Exception e) {
            populateTimestamp(dashboardData, "Certificates", LocalDateTime.now(clock));
            log.error("Failed to populate certificates", e);
            return Job.Result.failed("Failed to populate certificates. Unexpected exception", e);
        }
    }
}

