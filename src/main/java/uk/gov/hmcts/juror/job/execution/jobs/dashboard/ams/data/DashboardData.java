package uk.gov.hmcts.juror.job.execution.jobs.dashboard.ams.data;

import lombok.Getter;
import uk.gov.hmcts.juror.job.execution.client.contracts.SchedulerServiceClient;
import uk.gov.hmcts.juror.job.execution.jobs.dashboard.ams.AmsDashboardConfig;
import uk.gov.hmcts.juror.job.execution.service.contracts.DatabaseService;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;


@Getter
public class DashboardData {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm:ss");

    private final BureauLettersAutomaticallyGenerated bureauLettersAutomaticallyGenerated;
    private final BureauLettersToBePrinted bureauLettersToBePrinted;
    private final PncCheck pncCheck;
    private final Expenses expenses;
    private final Certificates certificates;
    private final Timestamps timestamps;

    private final List<DashboardDataEntry> dashboardDataEntries;

    public DashboardData(SchedulerServiceClient schedulerServiceClient,
                         DatabaseService databaseService, AmsDashboardConfig config, Clock clock) {

        this.bureauLettersAutomaticallyGenerated =
            new BureauLettersAutomaticallyGenerated(this, databaseService, config.getDatabase(), clock);
        this.bureauLettersToBePrinted =
            new BureauLettersToBePrinted(this, databaseService, config.getDatabase(), clock);
        this.pncCheck = new PncCheck(this, schedulerServiceClient);
        this.expenses = new Expenses(this, databaseService, config.getDatabase(), clock);
        this.certificates = new Certificates(this, config, clock);
        this.timestamps = new Timestamps(this);

        this.dashboardDataEntries = List.of(
            this.bureauLettersAutomaticallyGenerated,
            this.bureauLettersToBePrinted,
            this.pncCheck,
            this.expenses,
            this.certificates,
            this.timestamps
        );
    }

    public String toCsv(Clock clock) {
        StringBuilder builder = new StringBuilder(DATE_FORMATTER.format(LocalDateTime.now(clock)));
        for (DashboardDataEntry entry : this.getDashboardDataEntries()) {
            builder.append('\n');
            builder.append(entry.toCsv());
        }
        return builder.toString();
    }
}
