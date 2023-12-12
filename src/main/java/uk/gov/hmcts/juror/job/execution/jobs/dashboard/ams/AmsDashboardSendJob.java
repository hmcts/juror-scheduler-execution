package uk.gov.hmcts.juror.job.execution.jobs.dashboard.ams;

import lombok.Getter;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.juror.job.execution.jobs.LinearJob;
import uk.gov.hmcts.juror.job.execution.service.contracts.SftpService;

import java.util.Set;

@Component
@Getter
public class AmsDashboardSendJob extends LinearJob {
    private final SftpService sftpService;
    private final AmsDashboardConfig config;

    public AmsDashboardSendJob(SftpService sftpService,
                               AmsDashboardConfig config) {
        super();
        this.sftpService = sftpService;
        this.config = config;
    }

    @Override
    public ResultSupplier getResultSupplier() {
        return new ResultSupplier(false,
            Set.of(
                metaData -> sendDashboardFile()
            ));
    }

    Result sendDashboardFile() {
        if (sftpService.upload(AmsDashboardSftp.class, config.getDashboardCsvLocation())) {
            return Result.passed();
        } else {
            return Result.failed("Failed to upload dashboard file");
        }
    }
}
