package uk.gov.hmcts.juror.job.execution.jobs.dashboard.ams.data;

import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.juror.job.execution.config.DatabaseConfig;
import uk.gov.hmcts.juror.job.execution.jobs.Job;
import uk.gov.hmcts.juror.job.execution.service.contracts.DatabaseService;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@SuppressWarnings("PMD.LawOfDemeter")
public abstract class CommunicationCounts<T> extends DashboardDataEntry {
    final DatabaseService databaseService;
    final DatabaseConfig databaseConfig;
    final Clock clock;

    private final Class<T> rowType;
    private final String sql;
    private final String failureMessage;

    private final String errorText = "ERROR";

    protected CommunicationCounts(DashboardData dashboardData, String title,
                                  DatabaseService databaseService, DatabaseConfig databaseConfig, Clock clock,
                                  Class<T> rowType, String sql, String failureMessage) {
        super(dashboardData, title, "Type", "Description", "Count");
        this.databaseService = databaseService;
        this.databaseConfig = databaseConfig;
        this.clock = clock;
        this.rowType = rowType;
        this.sql = sql;
        this.failureMessage = failureMessage;
    }

    public void addRow(String type, String description, String count) {
        addEntry(type, description, count);
    }

    public abstract void addRow(T row);

    public Job.Result populate() {
        AtomicReference<Job.Result> result = new AtomicReference<>(Job.Result.passed());
        try {
            databaseService.execute(databaseConfig, connection -> {
                List<T> response = databaseService.executePreparedStatement(connection, rowType, sql);
                if (response == null || response.isEmpty()) {
                    addErrorRow();
                    result.set(Job.Result.failed("No response from database"));
                } else {
                    response.forEach(this::addRow);
                }
            });
        } catch (Exception e) {
            log.error("Unable to get {}", title, e);
            addErrorRow();
            result.set(Job.Result.failed(failureMessage, e));
        }
        populateTimestamp(dashboardData, title, LocalDateTime.now(clock));
        return result.get();
    }

    private void addErrorRow() {
        addRow(errorText, errorText, errorText);
    }
}
