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
public class BureauLettersAutomaticallyGenerated extends DashboardDataEntry {
    static final String BUREAU_AUTO_GEN_LETTERS_SQL = """
    select
        coalesce(sum(case when jh.history_code = 'RDIS' then 1 else 0 end),0) withdrawal,
        coalesce(sum(case when jh.history_code = 'RRES' then 1 else 0 end),0) confirmation
    from juror_mod.juror_history jh
    where jh.user_id = 'AUTO' and jh.date_created::date = current_date;
        """;
    final DatabaseService databaseService;
    final DatabaseConfig databaseConfig;
    final Clock clock;

    protected BureauLettersAutomaticallyGenerated(DashboardData dashboardData,
                                                  DatabaseService databaseService,
                                                  DatabaseConfig databaseConfig,
                                                  Clock clock) {
        super(dashboardData, "Bureau Letters Automatically Generated", "Withdrawal", "Confirmation");
        this.databaseService = databaseService;
        this.databaseConfig = databaseConfig;
        this.clock = clock;
    }

    public void addRow(String type, String count) {
        addEntry(type, count);
    }

    public void addRow(BureauLettersAutomaticallyGeneratedDB bureauLettersAutomaticallyGeneratedDB) {
        this.addRow("WITHDRAWAL", bureauLettersAutomaticallyGeneratedDB.getWithdrawal().toString());
        this.addRow("CONFIRMATION", bureauLettersAutomaticallyGeneratedDB.getConfirmation().toString());
    }

    public Job.Result populate() {
        final String errorText = "ERROR";
        AtomicReference<Job.Result> result = new AtomicReference<>(null);
        try {
            databaseService.execute(databaseConfig, connection -> {
                List<BureauLettersAutomaticallyGeneratedDB> response =
                    databaseService.executePreparedStatement(connection, BureauLettersAutomaticallyGeneratedDB.class,
                        BUREAU_AUTO_GEN_LETTERS_SQL);

                if (response == null || response.isEmpty()) {
                    addRow("Withdrawal", errorText);
                    addRow("Confirmation", errorText);
                    result.set(Job.Result.failed("No response from database"));
                } else {
                    response.forEach(this::addRow);
                }
            });
        } catch (Exception e) {
            addRow("Withdrawal", errorText);
            addRow("Confirmation", errorText);
            return Job.Result.failed("Failed to get Confirmation letter or withdraw letter information");
        }

        populateTimestamp(dashboardData, "Bureau Letters Automatically Generated", LocalDateTime.now(clock));
        if (result.get() == null) {
            return Job.Result.passed();
        } else {
            return result.get();
        }
    }
}
