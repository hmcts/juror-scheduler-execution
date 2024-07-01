package uk.gov.hmcts.juror.job.execution.jobs.dashboard.ams;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.juror.job.execution.client.contracts.SchedulerServiceClient;
import uk.gov.hmcts.juror.job.execution.database.DatabaseColumn;
import uk.gov.hmcts.juror.job.execution.jobs.LinearJob;
import uk.gov.hmcts.juror.job.execution.service.contracts.DatabaseService;

import java.time.Clock;
import java.util.List;

@Component
@Getter
@Slf4j
public class AmsDashboardGenerateJobStaticLog extends LinearJob {

    private final SchedulerServiceClient schedulerServiceClient;
    private final DatabaseService databaseService;
    private final AmsDashboardConfig config;
    private final Clock clock;

    @Autowired
    public AmsDashboardGenerateJobStaticLog(SchedulerServiceClient schedulerServiceClient,
                                            DatabaseService databaseService,
                                            AmsDashboardConfig config, Clock clock) {
        super();
        this.schedulerServiceClient = schedulerServiceClient;
        this.databaseService = databaseService;
        this.config = config;
        this.clock = clock;
    }

    @Override
    public ResultSupplier getResultSupplier() {
        return new ResultSupplier(true, List.of(
            metaData -> sendLog()
        ));
    }

    private Result sendLog() {
        databaseService.execute(config.getDatabase(),
            connection -> log.info(databaseService.executePreparedStatement(connection, ApplicationSettings.class,
                "SELECT * FROM juror_mod.app_setting WHERE setting = 'ams_dashboard_log'").get(0).getValue()));
        return Result.passed();
    }

    @Getter
    @Setter
    @Accessors(chain = true)
    public static class ApplicationSettings {
        @DatabaseColumn(name = "setting", setter = "setSetting")
        private String setting;
        @DatabaseColumn(name = "value", setter = "setValue")
        private String value;
    }
}