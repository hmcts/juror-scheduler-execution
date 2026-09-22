package uk.gov.hmcts.juror.job.execution.jobs.dashboard.ams;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.juror.job.execution.client.contracts.SchedulerServiceClient;
import uk.gov.hmcts.juror.job.execution.database.model.MetaData;
import uk.gov.hmcts.juror.job.execution.jobs.ParallelJob;
import uk.gov.hmcts.juror.job.execution.jobs.dashboard.ams.data.DashboardData;
import uk.gov.hmcts.juror.job.execution.model.Status;
import uk.gov.hmcts.juror.job.execution.service.contracts.DatabaseService;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

@Component
@Getter
@Slf4j
public class AmsDashboardGenerateJob extends ParallelJob {

    private final SchedulerServiceClient schedulerServiceClient;
    private final DatabaseService databaseService;
    private final AmsDashboardConfig config;
    private final Clock clock;

    @Autowired
    public AmsDashboardGenerateJob(SchedulerServiceClient schedulerServiceClient,
                                   DatabaseService databaseService,
                                   AmsDashboardConfig config, Clock clock) {
        super();
        this.schedulerServiceClient = schedulerServiceClient;
        this.databaseService = databaseService;
        this.config = config;
        this.clock = clock;
    }

    @Override
    @SuppressWarnings("PMD.AvoidLiteralsInIfCondition")
    public Result executeRunners(MetaData metaData) {
        DashboardData dashboardData = createDashboardData();
        List<Result> results = populateDashboardData(dashboardData, metaData);
        long successfulResults = results.stream()
            .filter(result -> Status.SUCCESS == result.getStatus())
            .count();
        Result mergedResult = Result.merge(results);

        if (successfulResults == 0) {
            return createResult(Status.FAILED, mergedResult);
        }

        Result generateDashboardFileResult = generateDashboardFile(dashboardData);
        if (Status.SUCCESS != generateDashboardFileResult.getStatus()) {
            return generateDashboardFileResult;
        }

        if (successfulResults == results.size()) {
            return createResult(Status.SUCCESS, mergedResult);
        }
        return createResult(Status.PARTIAL_SUCCESS, mergedResult);
    }

    @Override
    public List<ResultSupplier> getResultSuppliers() {
        DashboardData dashboardData = createDashboardData();
        return List.of(
            new ResultSupplier(
                true,
                List.of(
                    metaData -> dashboardData.getBureauLettersAutomaticallyGenerated().populate(),
                    metaData -> dashboardData.getBureauLettersToBePrinted().populate(),
                    metaData -> dashboardData.getPncCheck().populate(),
                    metaData -> dashboardData.getExpenses().populate(),
                    metaData -> dashboardData.getCertificates().populate()
                )
            ),
            new ResultSupplier(false,
                List.of(
                    metaData -> generateDashboardFile(dashboardData)
                )
            )
        );
    }

    DashboardData createDashboardData() {
        return new DashboardData(schedulerServiceClient, databaseService, config, clock);
    }

    List<Result> populateDashboardData(DashboardData dashboardData, MetaData metaData) {
        List<Result> results = Collections.synchronizedList(new ArrayList<>());
        List<Function<MetaData, Result>> resultRunners = List.of(
            ignoredMetaData -> dashboardData.getBureauLettersAutomaticallyGenerated().populate(),
            ignoredMetaData -> dashboardData.getBureauLettersToBePrinted().populate(),
            ignoredMetaData -> dashboardData.getPncCheck().populate(),
            ignoredMetaData -> dashboardData.getExpenses().populate(),
            ignoredMetaData -> dashboardData.getCertificates().populate()
        );
        resultRunners.parallelStream().forEach(resultRunner ->
            results.add(runJobStep(resultRunner, metaData)));
        return results;
    }

    Result createResult(Status status, Result mergedResult) {
        return new Result(status, mergedResult.getMessage(), mergedResult.getThrowable())
            .addMetaData(mergedResult.getMetaData());
    }

    Result generateDashboardFile(DashboardData dashboardData) {
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
