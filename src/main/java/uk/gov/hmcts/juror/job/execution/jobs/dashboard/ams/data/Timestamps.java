package uk.gov.hmcts.juror.job.execution.jobs.dashboard.ams.data;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Timestamps extends DashboardDataEntry {
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    protected Timestamps(DashboardData dashboardData) {
        super(dashboardData, "Timestamps", "Section", "Last Update Date");
    }

    public void addRow(String name, LocalDateTime lastUpdatedAt) {
        if (lastUpdatedAt == null) {
            this.addRow(name, "ERROR");
        } else {
            this.addRow(name, DATE_TIME_FORMATTER.format(lastUpdatedAt));
        }
    }

    public void addRow(String name, String lastUpdatedAt) {
        addEntry(name, lastUpdatedAt);
    }
}
