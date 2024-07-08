package uk.gov.hmcts.juror.job.execution.jobs.dashboard.ams.data;

import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.juror.job.execution.config.DatabaseConfig;
import uk.gov.hmcts.juror.job.execution.jobs.Job;
import uk.gov.hmcts.juror.job.execution.jobs.dashboard.DashboardDataEntry;
import uk.gov.hmcts.juror.job.execution.service.contracts.DatabaseService;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("PMD.LawOfDemeter")
@Slf4j
public class Expenses extends DashboardDataEntry<DashboardData> {
    public static final String EXPENSES_SQL = """
        select to_char(coalesce(max(pd.creation_date), current_date  - '1 day'::interval),'DD/MM/YYYY') date,
         coalesce (sum(expense_total),0) amount
         from juror_mod.payment_data pd
         where expense_file_name in
         (select document_id
         from juror_mod.content_store cs
         where cs.date_sent is not null
         and cs.date_on_q_for_send >= current_date - '1 day'::interval
         and cs.file_type = 'PAYMENT')
         and pd.creation_date >= current_date - '1 day'::interval
         group by pd.creation_date
        """;
    final DatabaseService databaseService;
    final DatabaseConfig databaseConfig;
    final Clock clock;

    public Expenses(DashboardData dashboardData, DatabaseService databaseService,
                    DatabaseConfig databaseConfig,
                    Clock clock) {
        super(dashboardData, "Expenses", "Type", "Date", "Amount");
        this.databaseService = databaseService;
        this.databaseConfig = databaseConfig;
        this.clock = clock;
    }

    public void addRow(String date, String amount) {
        this.addEntry("Payment file total", date, amount);
    }

    public Job.Result populate() {
        final String errorText = "ERROR";
        AtomicReference<Job.Result> result = new AtomicReference<>(null);
        try {
            databaseService.execute(databaseConfig, connection -> {
                List<ExpensesDB> response =
                    databaseService.executePreparedStatement(connection, ExpensesDB.class, EXPENSES_SQL);
                if (response == null || response.isEmpty()) {
                    addRow(errorText, errorText);
                    result.set(Job.Result.failed("No response from database"));
                } else {
                    ExpensesDB expensesDB = response.get(0);
                    addRow(
                        expensesDB.getDate(), expensesDB.getAmount().toString()
                    );
                }
            });
        } catch (Exception e) {
            log.error("Unable to get Payment totals (expenses)", e);
            addRow(errorText, errorText);
            result.set(Job.Result.failed("Unexpected exception", e));
        }
        populateTimestamp(dashboardData, "Expenses", LocalDateTime.now(clock));
        if (result.get() == null) {
            return Job.Result.passed();
        } else {
            return result.get();
        }
    }
}
