package uk.gov.hmcts.juror.job.execution.jobs.contentstore.print;

import org.apache.commons.lang3.RandomUtils;
import uk.gov.hmcts.juror.job.execution.jobs.contentstore.ContentStoreFileJobTest;

import java.io.IOException;
import java.nio.file.Files;

import static org.mockito.Mockito.spy;

public class PrintFileJobTest extends ContentStoreFileJobTest {

    private PrintConfig config;

    public PrintFileJobTest() {
        super();
    }

    @Override
    protected PrintFileJob getContentStoreFileJob() throws IOException {
        this.config = new PrintConfig();
        this.config.setDatabase(createDatabaseConfig());
        this.config.setPrintFileRowLimit(RandomUtils.nextInt());
        this.config.setFtpDirectory(Files.createTempDirectory("PrintFileJobTest").toFile());

        this.ftpDirectory = this.config.getFtpDirectory();
        this.databaseConfig = this.config.getDatabase();
        this.fileType = "PRINT";
        this.procedureName = "printfiles_to_clob";
        this.procedureArguments = new Object[]{this.config.getPrintFileRowLimit()};
        this.fileNameRegex = "JURY\\d+\\.\\d+.*";
        this.sftpClass = PrintSftp.class;
        return spy(new PrintFileJob(
            config,
            sftpService,
            databaseService
        ));
    }
}
