package uk.gov.hmcts.juror.job.execution.jobs.contentstore.payment;

import uk.gov.hmcts.juror.job.execution.jobs.contentstore.ContentStoreFileJobTest;

import java.io.IOException;
import java.nio.file.Files;

import static org.mockito.Mockito.spy;

public class PaymentFileJobTest extends ContentStoreFileJobTest {

    private PaymentConfig config;

    public PaymentFileJobTest() {
        super();
    }

    @Override
    protected PaymentFileJob getContentStoreFileJob() throws IOException {
        this.config = new PaymentConfig();
        this.config.setDatabase(createDatabaseConfig());
        this.config.setFtpDirectory(Files.createTempDirectory("PaymentFileJobTest").toFile());
        this.ftpDirectory = this.config.getFtpDirectory();
        this.databaseConfig = this.config.getDatabase();
        this.fileType = "PAYMENT";
        this.procedureName = "payment_files_to_clob_extract";
        this.procedureArguments = new Object[]{};
        this.fileNameRegex = "JURY\\d+\\.\\d+.*";
        this.sftpClass = PaymentSftp.class;
        return spy(new PaymentFileJob(
            config,
            sftpService,
            databaseService
        ));
    }
}
