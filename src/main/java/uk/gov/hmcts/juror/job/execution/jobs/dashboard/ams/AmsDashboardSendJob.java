package uk.gov.hmcts.juror.job.execution.jobs.dashboard.ams;

import lombok.Getter;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.juror.job.execution.jobs.LinearJob;

import java.util.Set;

@Component
@Getter
public class AmsDashboardSendJob extends LinearJob {
    private final AmsDashboardConfig config;

    public AmsDashboardSendJob(AmsDashboardConfig config) {
        super();
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
        //todo - might need to remove this as dashboard is going to stdout
        return Result.passed();
    }
}
