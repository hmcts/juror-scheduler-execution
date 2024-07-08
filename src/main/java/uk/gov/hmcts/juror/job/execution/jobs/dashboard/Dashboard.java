package uk.gov.hmcts.juror.job.execution.jobs.dashboard;

import lombok.Getter;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Getter
public abstract class Dashboard {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm:ss");

    protected List<DashboardDataEntry<?>> dashboardDataEntries;

    public String toCsv(Clock clock) {
        StringBuilder builder = new StringBuilder(DATE_FORMATTER.format(LocalDateTime.now(clock)));
        for (DashboardDataEntry<?> entry : this.getDashboardDataEntries()) {
            builder.append('\n');
            builder.append(entry.toCsv());
        }
        return builder.toString();
    }
}
