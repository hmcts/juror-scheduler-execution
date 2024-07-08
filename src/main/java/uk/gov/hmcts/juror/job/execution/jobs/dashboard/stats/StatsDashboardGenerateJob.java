package uk.gov.hmcts.juror.job.execution.jobs.dashboard.stats;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.juror.job.execution.client.contracts.SchedulerServiceClient;
import uk.gov.hmcts.juror.job.execution.jobs.ParallelJob;
import uk.gov.hmcts.juror.job.execution.jobs.dashboard.stats.data.StatsDashboardData;

import java.time.Clock;
import java.util.List;

@Component
@Getter
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class StatsDashboardGenerateJob extends ParallelJob {
    private final Clock clock;
    private final SchedulerServiceClient schedulerServiceClient;

    @Override
    public List<ResultSupplier> getResultSuppliers() {
        StatsDashboardData dashboardData = new StatsDashboardData(schedulerServiceClient);
        return List.of(
            new ResultSupplier(
                true,
                List.of(
                    metaData -> dashboardData.getCommunicationServiceInfo().populate()
                )
            ),
            new ResultSupplier(false,
                List.of(
                    metaData -> generateDashboardFile(dashboardData)
                )
            )
        );
    }

    Result generateDashboardFile(StatsDashboardData dashboardData) {
        try {
            String dashboardCsv = dashboardData.toCsv(clock);
            log.info(dashboardCsv);
            return Result.passed();
        } catch (Exception e) {
            log.error("Failed to output dashboard csv", e);
            return Result.failed("Failed to output dashboard csv", e);
        }
    }
}