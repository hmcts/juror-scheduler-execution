package uk.gov.hmcts.juror.job.execution.jobs.contentstore.payment;

import uk.gov.hmcts.juror.job.execution.jobs.contentstore.ContentStoreFileJobTest;

import java.io.IOException;
import java.nio.file.Files;

import static org.mockito.Mockito.spy;

public class PaymentFileJobTest extends ContentStoreFileJobTest {

    public PaymentFileJobTest() {
        super();
    }

    @Override
    protected PaymentFileJob getContentStoreFileJob() throws IOException {
        PaymentConfig config = new PaymentConfig();
        config.setDatabase(createDatabaseConfig());
        config.setFtpDirectory(Files.createTempDirectory("PaymentFileJobTest").toFile());
        this.ftpDirectory = config.getFtpDirectory();
        this.databaseConfig = config.getDatabase();
        this.fileType = "PAYMENT";
        this.procedureName = "payment_files_to_clob";
        this.procedureArguments = new Object[]{};
        this.fileNameRegex = "JUROR_\\d+.*\\d{13}\\.dat";
        this.sftpClass = PaymentSftp.class;
        return spy(new PaymentFileJob(
            config,
            sftpService,
            databaseService
        ));
    }
}
