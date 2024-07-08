package uk.gov.hmcts.juror.job.execution.jobs.dashboard.stats.data;

import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.juror.job.execution.client.contracts.SchedulerServiceClient;
import uk.gov.hmcts.juror.job.execution.jobs.Job;
import uk.gov.hmcts.juror.job.execution.jobs.dashboard.DashboardDataEntry;
import uk.gov.hmcts.juror.job.execution.model.Status;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.Function;

@Slf4j
public class CommunicationServiceInfo extends DashboardDataEntry<StatsDashboardData> {

    private final SchedulerServiceClient schedulerServiceClient;
    private static final String NOT_APPLICABLE = "N/A";
    private static final String ERROR_TEXT = "ERROR";

    protected CommunicationServiceInfo(StatsDashboardData dashboardData,
                                       SchedulerServiceClient schedulerServiceClient) {
        super(dashboardData, "Communication Service Information",
            "Name", "Identified 15 min", "Sent 15 min", "Fail 15 min",
            "Identified Total", " Sent Total", "Failed Total",
            "Last Run Successful");
        this.schedulerServiceClient = schedulerServiceClient;
    }

    public Job.Result populate() {
        loadCourtTextAndEmail();
        loadOneWeekComms();
        loadFourWeekComms();
        loadLetterComms();
        loadExcusedCompleted();
        loadSmartSurvey();
        loadSuperUrgent();
        return Job.Result.passed();
    }

    private void addEntry(Entry entry) {
        addEntry(entry.name, entry.identified15Min, entry.sent15Min, entry.failed15Min,
            entry.identifiedTotal, entry.sentTotal, entry.failedTotal, String.valueOf(entry.lastRunSuccessful)
        );
    }


    private void loadSuperUrgent() {
        TaskResponseResult taskResponseResult =
            getTaskResponse("CRONBATCH_URGENT_SUPER_URGENT_STATUS",
                Duration.of(25, ChronoUnit.HOURS)); //Job Runs every 24 hours
        processTaskResponseResult(
            "Super Urgent",
            taskResponseResult,
            taskResponse -> getMetaData(taskResponse, "TOTAL_MARKED_URGENT", ERROR_TEXT),
            taskResponse -> NOT_APPLICABLE,
            taskResponse -> NOT_APPLICABLE);
    }


    private void loadSmartSurvey() {
        TaskResponseResult taskResponseResult =
            getTaskResponse("CRONBATCH_SMART_SURVEY_IMPORT",
                Duration.of(25, ChronoUnit.HOURS)); //Job Runs every 24 hours
        processTaskResponseResult(
            "Smart Survey",
            taskResponseResult,
            taskResponse -> getMetaData(taskResponse, "RECORDS_INSERTED", ERROR_TEXT),
            taskResponse -> NOT_APPLICABLE,
            taskResponse -> getMetaData(taskResponse, "TOTAL_FAILED_TO_FIND", ERROR_TEXT));
    }

    private void loadExcusedCompleted() {
        final Duration duration;
        final DayOfWeek dayOfWeek = LocalDate.now().getDayOfWeek();
        if (dayOfWeek == DayOfWeek.SUNDAY) {
            //49 hours when the job runs on a sunday (As last run would have been friday morning)
            duration = Duration.of(49, ChronoUnit.HOURS);
        } else if (dayOfWeek == DayOfWeek.MONDAY) {
            //73 hours when the job runs on a sunday (As last run would have been friday morning)
            duration = Duration.of(73, ChronoUnit.HOURS);
        } else {
            duration = Duration.of(25, ChronoUnit.HOURS); //25 hours when the job runs on a weekday
        }
        TaskResponseResult taskResponseResult =
            getTaskResponse("CRONBATCH_EXCUSAL_COMPLETED_SERVICE_COURT_COMMS", duration); //Job Runs every Mon-Fri
        processTaskResponseResult(
            "Excused/Completed",
            taskResponseResult,
            taskResponse -> addMetaData(taskResponse, "COMPLETED_IDENTIFIED", "EXCUSAL_IDENTIFIED"),
            taskResponse -> addMetaData(taskResponse, "COMPLETED_SUCCESS_COUNT", "EXCUSAL_SUCCESS_COUNT"),
            taskResponse -> addMetaData(taskResponse, "COMPLETED_ERROR_COUNT", "EXCUSAL_ERROR_COUNT",
                "COMPLETED_MISSING_EMAIL_PHONE", "EXCUSAL_MISSING_EMAIL_PHONE"));
    }


    private void loadLetterComms() {
        TaskResponseResult taskResponseResult =
            getTaskResponse("CRONBATCH_LETTER_COMMS",
                Duration.of(25, ChronoUnit.HOURS)); //Job Runs every 24 hours
        processTaskResponseResult(
            "Letter Comms",
            taskResponseResult,
            taskResponse -> addMetaData(taskResponse, "COMMNS_SENT", "COMMS_FAILED", ERROR_TEXT),
            taskResponse -> getMetaData(taskResponse, "COMMNS_SENT"),
            taskResponse -> getMetaData(taskResponse, "COMMS_FAILED"));
    }

    private void loadOneWeekComms() {
        final Duration duration;
        final DayOfWeek dayOfWeek = LocalDate.now().getDayOfWeek();
        if (dayOfWeek == DayOfWeek.SUNDAY) {
            //49 hours when the job runs on a sunday (As last run would have been friday evening)
            duration = Duration.of(49, ChronoUnit.HOURS);
        } else if (dayOfWeek == DayOfWeek.MONDAY) {
            //73 hours when the job runs on a sunday (As last run would have been friday evening)
            duration = Duration.of(73, ChronoUnit.HOURS);
        } else if (dayOfWeek == DayOfWeek.FRIDAY) {
            //49 hours when the job runs on a sunday (As last run would have been wednesday evening)
            duration = Duration.of(49, ChronoUnit.HOURS);
        } else {
            duration = Duration.of(25, ChronoUnit.HOURS); //25 hours when the job runs on a weekday
        }
        TaskResponseResult taskResponseResult =
            getTaskResponse("CRONBATCH_WEEKLY_COMMS", duration); //Job Runs every Mon-Fri (Excluding Thursday)
        processTaskResponseResult(
            "Weekly Comms",
            taskResponseResult,
            taskResponse -> addMetaData(taskResponse, "INFO_COMMS_SENT", "INFO_COMMS_FAILED", "NO_EMAIL_ADDRESS"),
            taskResponse -> getMetaData(taskResponse, "INFO_COMMS_SENT"),
            taskResponse -> addMetaData(taskResponse, "INFO_COMMS_FAILED", "NO_EMAIL_ADDRESS"));
    }

    private void loadFourWeekComms() {
        //Job Runs every 7 days (168 hours)
        final Duration duration = Duration.of(169, ChronoUnit.HOURS);
        TaskResponseResult taskResponseResult =
            getTaskResponse("CRONBATCH_SEND_TO_COURT_COMMS", duration); //Job Runs every Thursday
        processTaskResponseResult(
            "One Week Comms Email",
            taskResponseResult,
            taskResponse -> addMetaData(taskResponse, "SUCCESS_COUNT_EMAIL", "ERROR_COUNT_EMAIL"),
            taskResponse -> getMetaData(taskResponse, "SUCCESS_COUNT_EMAIL"),
            taskResponse -> getMetaData(taskResponse, "ERROR_COUNT_EMAIL"));

        processTaskResponseResult(
            "One Week Comms SMS",
            taskResponseResult,
            taskResponse -> addMetaData(taskResponse, "SUCCESS_COUNT_SMS", "ERROR_COUNT_SMS"),
            taskResponse -> getMetaData(taskResponse, "SUCCESS_COUNT_SMS"),
            taskResponse -> getMetaData(taskResponse, "ERROR_COUNT_SMS"));
    }

    private void loadCourtTextAndEmail() {
        final Duration duration;
        LocalDateTime currentTime = LocalDateTime.now();
        //If the time is outside normal operating hours make sure last run was at least 24 hours ago
        if (currentTime.isAfter(LocalTime.of(19, 0).atDate(LocalDate.now()))
            || currentTime.isBefore(LocalTime.of(7, 0).atDate(LocalDate.now()))) {
            duration = Duration.of(25, ChronoUnit.HOURS);
        } else {
            duration = Duration.of(20, ChronoUnit.MINUTES);
        }

        TaskResponseResult taskResponseResult =
            getTaskResponse("CRONBATCH_COURT_COMMS", duration); //Job Runs 15 mins every Everyday 7-19


        List<SchedulerServiceClient.TaskResponse> taskResponseResultsToDay = schedulerServiceClient.searchTasks(
            SchedulerServiceClient.TaskSearch.builder()
                .jobKey("CRONBATCH_COURT_COMMS")
                .fromDate(LocalDate.now().atStartOfDay())
                .build()
        );

        processTaskResponseResult(
            "Court Text and Email",
            taskResponseResult,
            true,
            taskResponse -> getMetaData(taskResponse, "TOTAL_MESSAGES_TO_SEND"),
            taskResponse -> addMetaData(taskResponse, "EMAIL_SUCCESS", "SMS_SUCCESS"),
            taskResponse -> addMetaData(taskResponse,
                "ERROR_COUNT", "MISSING_API_KEY_COUNT", "MISSING_EMAIL_AND_PHONE"),

            taskResponse -> addMetaDataList(taskResponseResultsToDay, "TOTAL_MESSAGES_TO_SEND"),
            taskResponse -> addMetaDataList(taskResponseResultsToDay, "EMAIL_SUCCESS", "SMS_SUCCESS"),
            taskResponse -> addMetaDataList(taskResponseResultsToDay,
                "ERROR_COUNT", "MISSING_API_KEY_COUNT", "MISSING_EMAIL_AND_PHONE"));
    }


    private TaskResponseResult getTaskResponse(String jobKey, Duration mustHaveRunWithin) {
        SchedulerServiceClient.TaskResponse taskResponse = schedulerServiceClient.getLatestTask(jobKey);

        if (taskResponse == null) {
            return new TaskResponseResult(null, false, false, false);
        }

        boolean withinTime = taskResponse.getCreatedAt().isBefore(taskResponse.getCreatedAt().minus(mustHaveRunWithin));
        boolean wasSuccessful = taskResponse.getStatus() == Status.SUCCESS;

        return new TaskResponseResult(taskResponse,
            true,
            withinTime,
            wasSuccessful
        );
    }

    private record TaskResponseResult(SchedulerServiceClient.TaskResponse taskResponse,
                                      boolean taskResponseFound,
                                      boolean withinTime,
                                      boolean wasSuccessful) {
    }

    private record Entry(String name, String identified15Min, String sent15Min, String failed15Min,
                         String identifiedTotal, String sentTotal, String failedTotal,
                         boolean lastRunSuccessful) {
    }

    private String addMetaDataList(List<SchedulerServiceClient.TaskResponse> taskResponses,
                                   String... keys) {
        try {
            int total = 0;
            for (SchedulerServiceClient.TaskResponse taskResponse : taskResponses) {
                total += Integer.parseInt(addMetaData(taskResponse, keys));
            }
            return String.valueOf(total);
        } catch (Exception exception) {
            log.error("Failed to get metadata as integer", exception);
            return ERROR_TEXT;
        }
    }

    private String addMetaData(SchedulerServiceClient.TaskResponse taskResponse,
                               String... keys) {
        try {
            int total = 0;
            for (String key : keys) {
                total += Integer.parseInt(getMetaData(taskResponse, key, ERROR_TEXT));
            }
            return String.valueOf(total);
        } catch (Exception exception) {
            log.error("Failed to get metadata as integer", exception);
            return ERROR_TEXT;
        }
    }

    private void processTaskResponseResult(
        String title,
        TaskResponseResult taskResponseResult,
        Function<SchedulerServiceClient.TaskResponse, String> identifierTotal,
        Function<SchedulerServiceClient.TaskResponse, String> sentTotal,
        Function<SchedulerServiceClient.TaskResponse, String> failedTotal
    ) {
        processTaskResponseResult(title,
            taskResponseResult,
            false,
            taskResponse -> NOT_APPLICABLE,
            taskResponse -> NOT_APPLICABLE,
            taskResponse -> NOT_APPLICABLE,
            identifierTotal,
            sentTotal,
            failedTotal);
    }

    private void processTaskResponseResult(
        String title,
        TaskResponseResult taskResponseResult,
        boolean is15MinCheck,
        Function<SchedulerServiceClient.TaskResponse, String> identifier15Min,
        Function<SchedulerServiceClient.TaskResponse, String> sent15Min,
        Function<SchedulerServiceClient.TaskResponse, String> failed15Min,
        Function<SchedulerServiceClient.TaskResponse, String> identifierTotal,
        Function<SchedulerServiceClient.TaskResponse, String> sentTotal,
        Function<SchedulerServiceClient.TaskResponse, String> failedTotal
    ) {

        final String failureText = "FAIL";
        String checkText15Mins = is15MinCheck ? failureText : NOT_APPLICABLE;
        if (!taskResponseResult.taskResponseFound) {
            addEntry(new Entry(title,
                checkText15Mins,
                checkText15Mins,
                checkText15Mins,
                failureText,
                failureText,
                failureText,
                false));
        } else {
            addEntry(new Entry(title,
                identifier15Min.apply(taskResponseResult.taskResponse),
                sent15Min.apply(taskResponseResult.taskResponse),
                failed15Min.apply(taskResponseResult.taskResponse),
                identifierTotal.apply(taskResponseResult.taskResponse),
                sentTotal.apply(taskResponseResult.taskResponse),
                failedTotal.apply(taskResponseResult.taskResponse),
                taskResponseResult.withinTime && taskResponseResult.wasSuccessful));
        }
    }

    private String getMetaData(SchedulerServiceClient.TaskResponse taskResponse, String key) {
        return getMetaData(taskResponse, key, "ERROR");
    }

    private String getMetaData(SchedulerServiceClient.TaskResponse taskResponse, String key, String _default) {
        return taskResponse.getMetaData().getOrDefault(key, _default);
    }
}
