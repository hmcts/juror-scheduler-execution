package uk.gov.hmcts.juror.job.execution.jobs.dashboard.ams.data;

import uk.gov.hmcts.juror.job.execution.config.DatabaseConfig;
import uk.gov.hmcts.juror.job.execution.jobs.Job;
import uk.gov.hmcts.juror.job.execution.service.contracts.DatabaseService;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("PMD.LawOfDemeter")
public class Expenses extends DashboardDataEntry {
    public static final String EXPENSES_SQL = """
        select to_char(Nvl(max(trunc(creation_date)),trunc(sysdate-1)),'DD/MM/YYYY') date,
        Nvl(sum(expense_total),0) amount
        from aramis_payments
        where con_file_ref in
        (select document_id from content_store
         where trunc(date_sent) is not null
         and date_on_q_for_send >= trunc(sysdate-1)
         and file_type = 'PAYMENT')
        and creation_date >= trunc(sysdate-1)
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
                        expensesDB.getDate(), expensesDB.getAmount()
                    );
                }
            });
        } catch (Exception e) {
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
