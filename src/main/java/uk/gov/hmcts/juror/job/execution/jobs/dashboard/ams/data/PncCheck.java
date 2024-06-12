package uk.gov.hmcts.juror.job.execution.jobs.dashboard.ams.data;

import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.juror.job.execution.client.contracts.SchedulerServiceClient;
import uk.gov.hmcts.juror.job.execution.jobs.Job;
import uk.gov.hmcts.juror.job.execution.jobs.checks.pnc.batch.PncBatchJob;
import uk.gov.hmcts.juror.job.execution.jobs.checks.pnc.batch.PoliceCheck;

import java.time.LocalDateTime;

@Slf4j
@SuppressWarnings("PMD.LawOfDemeter")
public class PncCheck extends DashboardDataEntry {
    final SchedulerServiceClient schedulerServiceClient;

    public PncCheck(DashboardData dashboardData,
                    SchedulerServiceClient schedulerServiceClient) {
        super(dashboardData, "PNC Checks", "Type", "Submitted", "Completed", "Failed", "Unchecked");
        this.schedulerServiceClient = schedulerServiceClient;
    }

    @SuppressWarnings("PMD.UseObjectForClearerAPI")
    public void addRow(String type, String submitted, String completed, String failed, String unchecked) {
        this.addEntry(type,
            submitted,
            completed,
            failed,
            unchecked);
    }

    public Job.Result populate() {
        final String errorText = "ERROR";
        final String bureauText = "Bureau";
        LocalDateTime lastUpdatedAt = null;
        Job.Result result;
        try {
            SchedulerServiceClient.TaskResponse task =
                schedulerServiceClient.getLatestTask("PNC_BATCH");

            if (task == null) {
                this.addRow(bureauText, errorText, errorText, errorText, errorText);
                result = Job.Result.failed("Failed to get PNC check results");
            } else {
                String submitted = task.getMetaData().getOrDefault(PncBatchJob.TOTAL_CHECKS_REQUESTED_KEY, "0");

                Long eligible = getPoliceCheckValue(task, PoliceCheck.ELIGIBLE);
                Long ineligible = getPoliceCheckValue(task, PoliceCheck.INELIGIBLE);
                Long maxRetriesExceeded = getPoliceCheckValue(task, PoliceCheck.UNCHECKED_MAX_RETRIES_EXCEEDED);


                addRow(bureauText, submitted, String.valueOf(eligible + ineligible), String.valueOf(ineligible),
                    String.valueOf(maxRetriesExceeded));
                lastUpdatedAt = task.getLastUpdatedAt();
                result = Job.Result.passed();
            }
        } catch (Exception e) {
            this.addRow(bureauText, errorText, errorText, errorText, errorText);
            result = Job.Result.failed("Failed to get PNC check results", e);
        }
        addRow("Court", "0", "0", "0", "0");
        populateTimestamp(dashboardData, "PNC Checks", lastUpdatedAt);
        return result;
    }

    Long getPoliceCheckValue(SchedulerServiceClient.TaskResponse task, PoliceCheck policeCheck) {
        return Long.parseLong(
            task.getMetaData().getOrDefault("TOTAL_WITH_STATUS_" + policeCheck.name(), "0"));
    }
}
