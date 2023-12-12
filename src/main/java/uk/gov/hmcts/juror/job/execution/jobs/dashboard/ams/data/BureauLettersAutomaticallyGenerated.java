package uk.gov.hmcts.juror.job.execution.jobs.dashboard.ams.data;

import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.juror.job.execution.client.contracts.SchedulerServiceClient;
import uk.gov.hmcts.juror.job.execution.jobs.Job;
import uk.gov.hmcts.juror.job.execution.jobs.letter.LetterJob;

import java.time.LocalDateTime;

@Slf4j
@SuppressWarnings("PMD.LawOfDemeter")
public class BureauLettersAutomaticallyGenerated extends DashboardDataEntry {
    final SchedulerServiceClient schedulerServiceClient;

    protected BureauLettersAutomaticallyGenerated(DashboardData dashboardData,
                                                  SchedulerServiceClient schedulerServiceClient) {
        super(dashboardData, "Bureau Letters Automatically Generated", "Type", "Count");
        this.schedulerServiceClient = schedulerServiceClient;
    }

    public void addRow(String type, String count) {
        addEntry(type, count);
    }


    public Job.Result populate() {
        LocalDateTime confirmLetterLastChangedTime = addBureauLettersAutomaticallyGeneratedValue("CONFIRMATION",
            "CONFIRM_LETTER");
        LocalDateTime withdrawLetterLastChangedTime = addBureauLettersAutomaticallyGeneratedValue("WITHDRAW_LETTER",
            "WITHDRAW_LETTER");

        LocalDateTime lastUpdatedAt = getLatestDate(confirmLetterLastChangedTime, withdrawLetterLastChangedTime);
        if (lastUpdatedAt == null) {
            lastUpdatedAt = LocalDateTime.MIN;
        }
        populateTimestamp(dashboardData, "Bureau Letters Automatically Generated", lastUpdatedAt);
        if (confirmLetterLastChangedTime == null || withdrawLetterLastChangedTime == null) {
            return Job.Result.failed("Failed to get Confirmation letter or withdraw letter information");

        }
        return Job.Result.passed();
    }

    LocalDateTime addBureauLettersAutomaticallyGeneratedValue(String type,
                                                              String jobKey) {
        try {
            SchedulerServiceClient.TaskResponse task =
                schedulerServiceClient.getLatestTask(jobKey);
            if (task != null) {
                addRow(
                    type,
                    task.getMetaData().getOrDefault(LetterJob.TOTAL_LETTERS_GENERATED_META_DATA_KEY, "ERROR")
                );
                return task.getLastUpdatedAt();
            }
        } catch (Exception e) {
            log.error("Failed to get latest task for jobKey: " + jobKey, e);
        }
        addRow(type, "ERROR");
        return null;
    }
}
