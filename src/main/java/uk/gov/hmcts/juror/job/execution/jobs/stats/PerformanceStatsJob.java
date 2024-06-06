package uk.gov.hmcts.juror.job.execution.jobs.stats;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.juror.job.execution.jobs.ParallelJob;
import uk.gov.hmcts.juror.job.execution.service.contracts.DatabaseService;

import java.util.List;
import java.util.Set;

@Component
@Getter
public class PerformanceStatsJob extends ParallelJob {
    private final PerformanceStatsConfig config;
    private final DatabaseService databaseService;

    @Autowired
    public PerformanceStatsJob(DatabaseService databaseService,
                               PerformanceStatsConfig config) {
        super();
        this.databaseService = databaseService;
        this.config = config;
    }

    @Override
    public List<ResultSupplier> getResultSuppliers() {
        return List.of(
            new ResultSupplier(false,
                Set.of(metaData -> runRunProcedure("refresh_stats_data.auto_processed"))),
            new ResultSupplier(false,
                Set.of(
                    metaData -> runRunProcedure("refresh_stats_data.response_times_and_non_respond",
                        this.config.getResponseTimesAndNonRespondNoMonths()),
                    metaData -> runRunProcedure("refresh_stats_data.unprocessed_responses"),
                    metaData -> runRunProcedure("refresh_stats_data.welsh_online_responses",
                        this.config.getWelshOnlineResponsesNoMonths()),
                    metaData -> runRunProcedure("refresh_stats_data.thirdparty_online",
                        this.config.getThirdpartyOnlineNoMonths()),
                    metaData -> runRunProcedure("refresh_stats_data.deferrals",
                        this.config.getDeferralsNoMonths()),
                    metaData -> runRunProcedure("refresh_stats_data.excusals",
                        this.config.getExcusalsNoMonths())
                )
            )
        );
    }

    Result runRunProcedure(String procedureName, Object... arguments) {
        this.databaseService.executeStoredProcedure(this.config.getDatabase(), procedureName, arguments);
        return Result.passed("PASSED: " + procedureName);
    }
}