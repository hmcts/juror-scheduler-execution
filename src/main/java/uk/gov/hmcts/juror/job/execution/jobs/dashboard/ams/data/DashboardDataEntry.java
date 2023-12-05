package uk.gov.hmcts.juror.job.execution.jobs.dashboard.ams.data;

import uk.gov.hmcts.juror.standard.service.exceptions.InternalServerException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class DashboardDataEntry {
    protected final String title;

    protected final DashboardData dashboardData;

    protected final List<String[]> rows;

    protected final int columCount;

    protected DashboardDataEntry(DashboardData dashboardData, String title, String... columns) {
        this.dashboardData = dashboardData;
        this.title = title;
        this.rows = Collections.synchronizedList(new ArrayList<>());
        this.rows.add(columns);
        this.columCount = columns.length;
    }

    protected void addEntry(String... values) {
        if (values.length != columCount) {
            throw new InternalServerException("Number of values must match number of columns");
        }
        this.rows.add(values);
    }

    public static LocalDateTime getLatestDate(LocalDateTime date1, LocalDateTime date2) {
        if (date1 == null && date2 == null) {
            return null;
        }
        if (date1 == null ^ date2 == null) {
            return date1 != null
                ? date1
                : date2;
        }
        if (date1.isAfter(date2)) {
            return date1;
        }
        return date2;
    }

    protected void populateTimestamp(DashboardData dashboardData, String section, LocalDateTime lastUpdatedAt) {
        dashboardData.getTimestamps().addRow(section, lastUpdatedAt);
    }

    protected void populateTimestamp(DashboardData dashboardData, String section, String message) {
        dashboardData.getTimestamps().addRow(section, message);
    }

    public StringBuilder toCsv() {
        StringBuilder builder = new StringBuilder("[")
            .append(title)
            .append(']');

        for (String[] row : rows) {
            builder.append('\n')
                .append(String.join(",", row));
        }
        return builder;
    }
}
