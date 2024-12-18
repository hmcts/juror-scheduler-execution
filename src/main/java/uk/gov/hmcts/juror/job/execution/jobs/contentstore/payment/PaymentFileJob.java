package uk.gov.hmcts.juror.job.execution.jobs.contentstore.payment;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.juror.job.execution.jobs.contentstore.ContentStoreFileJob;
import uk.gov.hmcts.juror.job.execution.service.contracts.DatabaseService;
import uk.gov.hmcts.juror.job.execution.service.contracts.SftpService;

@Component
@Slf4j
public class PaymentFileJob extends ContentStoreFileJob {
    @Autowired
    public PaymentFileJob(
        PaymentConfig paymentConfig,
        SftpService sftpService,
        DatabaseService databaseService) {
        super(sftpService,
            databaseService,
            paymentConfig.getFtpDirectory(),
            paymentConfig.getDatabase(),
            "PAYMENT",
            "payment_files_to_clob",
            new Object[0],
            "JUROR_\\d+.*\\d{12}\\.dat",
            PaymentSftp.class,
            paymentConfig.getRetryLimit(),
            paymentConfig.getRetryDelay());
    }
}
