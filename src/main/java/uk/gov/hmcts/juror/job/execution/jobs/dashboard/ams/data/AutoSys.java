package uk.gov.hmcts.juror.job.execution.jobs.dashboard.ams.data;

import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.juror.job.execution.client.contracts.SchedulerServiceClient;
import uk.gov.hmcts.juror.job.execution.jobs.Job;
import uk.gov.hmcts.juror.job.execution.jobs.dashboard.DashboardDataEntry;
import uk.gov.hmcts.juror.job.execution.model.Status;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Set;

@Slf4j
@SuppressWarnings("PMD.LawOfDemeter")
public class AutoSys extends DashboardDataEntry<DashboardData> {
    private static final Set<String> JOB_KEYS_TO_TRACK = Set.of(
        "PAYMENT_JOB",
        "PRINT_JOB",
        "POOL_TRANSFER",
        "PNC_BATCH"
    );
    final SchedulerServiceClient schedulerServiceClient;
    final Clock clock;

    protected AutoSys(DashboardData dashboardData,
                      SchedulerServiceClient schedulerServiceClient,
                      Clock clock) {
        super(dashboardData, "AUTOSYS LOG", "Job", "Status");
        this.schedulerServiceClient = schedulerServiceClient;
        this.clock = clock;
    }

    public void addRow(String job, String status) {
        addEntry(job, status);
    }


    public Job.Result populate() {
        LocalDateTime lastUpdatedAt = LocalDateTime.MIN;
        boolean allPassed = true;
        for (String job : JOB_KEYS_TO_TRACK) {
            SchedulerServiceClient.TaskResponse taskResponse = populate(job);
            if (taskResponse != null) {
                lastUpdatedAt = getLatestDate(lastUpdatedAt, taskResponse.getLastUpdatedAt());
                if (taskResponse.getStatus() != Status.SUCCESS) {
                    allPassed = false;
                }
            } else {
                allPassed = false;
            }
        }
        populateTimestamp(dashboardData, "AUTOSYS LOG", lastUpdatedAt);
        populateTimestamp(dashboardData, "Errors Overnight", lastUpdatedAt);
        if (allPassed) {
            dashboardData.getErrorsOvernight().addRow("SSUPVL03", "None");
            return Job.Result.passed();
        } else {
            dashboardData.getErrorsOvernight().addRow("SSUPVL03", "ERROR");
            return Job.Result.failed("One or more jobs failed");
        }
    }

    SchedulerServiceClient.TaskResponse populate(String jobKey) {
        LocalDateTime currentTime = LocalDateTime.now(clock);
        SchedulerServiceClient.TaskResponse taskResponse = null;

        try {
            taskResponse = schedulerServiceClient.getLatestTask(jobKey);
        } catch (Exception e) {
            log.error("Failed to get Latest task for: " + jobKey, e);
        }
        if (taskResponse == null
            || taskResponse.getCreatedAt().isBefore(currentTime.minusHours(24))) {
            addRow(jobKey, "ERROR");
        } else {
            addRow(jobKey, taskResponse.getStatus().name());
        }
        return taskResponse;
    }
}
