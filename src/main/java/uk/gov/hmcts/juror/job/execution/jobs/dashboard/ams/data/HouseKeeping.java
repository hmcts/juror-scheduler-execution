package uk.gov.hmcts.juror.job.execution.jobs.dashboard.ams.data;

import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.juror.job.execution.client.contracts.SchedulerServiceClient;
import uk.gov.hmcts.juror.job.execution.jobs.Job;
import uk.gov.hmcts.juror.job.execution.model.Status;

import java.time.Clock;
import java.time.LocalDateTime;

@Slf4j
public class HouseKeeping extends DashboardDataEntry {
    final SchedulerServiceClient schedulerServiceClient;
    final Clock clock;

    public HouseKeeping(DashboardData dashboardData,
                        SchedulerServiceClient schedulerServiceClient, Clock clock) {
        super(dashboardData, "Housekeeping", "Status", "Other Info");
        this.schedulerServiceClient = schedulerServiceClient;
        this.clock = clock;
    }

    public void addRow(String status, String otherInfo) {
        this.addEntry(status, otherInfo);
    }

    public Job.Result populate() {
        final String jobKey = "HOUSE_KEEPING";
        Job.Result result;
        try {
            SchedulerServiceClient.TaskResponse response = schedulerServiceClient.getLatestTask(jobKey);
            LocalDateTime sevenDaysAgo = LocalDateTime.now(clock).minusDays(7);
            if (response == null || response.getCreatedAt() == null) {
                addRow("ERROR", "House keeping Job not found");
                populateTimestamp(dashboardData, "Housekeeping", "ERROR");
                return Job.Result.failed("House keeping job not found");
            }
            if (response.getCreatedAt().isBefore(sevenDaysAgo)) {
                addRow("Failed", "Last run was run more then 7 days ago");
                result = Job.Result.failed("Last run was run more then 7 days ago");
            } else if (response.getStatus() != Status.SUCCESS) {
                addRow("Failed", "Status: " + response.getStatus());
                result = Job.Result.failed("Expected status 'SUCCESS' but was " + response.getStatus());
            } else {
                addRow("Success", "");
                result = Job.Result.passed();
            }
            populateTimestamp(dashboardData, "Housekeeping", response.getLastUpdatedAt());
            return result;
        } catch (Exception e) {
            addRow("Failed", "Unexpected exception");
            populateTimestamp(dashboardData, "Housekeeping", "ERROR");
            log.error("Unexpected exception when getting latest task for " + jobKey, e);
            return Job.Result.failed("Unexpected gateway exception when getting latest task for " + jobKey, e);
        }
    }
}
