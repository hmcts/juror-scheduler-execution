package uk.gov.hmcts.juror.job.execution.jobs.contentstore.print;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.juror.job.execution.jobs.contentstore.ContentStoreFileJob;
import uk.gov.hmcts.juror.job.execution.service.contracts.DatabaseService;
import uk.gov.hmcts.juror.job.execution.service.contracts.SftpService;

@Component
@Slf4j
public class PrintFileJob extends ContentStoreFileJob {
    @Autowired
    public PrintFileJob(
        PrintConfig printConfig,
        SftpService sftpService,
        DatabaseService databaseService) {
        super(sftpService,
            databaseService,
            printConfig.getFtpDirectory(),
            printConfig.getDatabase(),
            "PRINT",
            "printfiles_to_clob",
            new Object[]{printConfig.getPrintFileRowLimit()},
            "JURY\\d+\\.\\d+.*",
            PrintSftp.class,
            printConfig.getRetryLimit(),
            printConfig.getRetryDelay());
    }
}
