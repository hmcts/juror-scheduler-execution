package uk.gov.hmcts.juror.job.execution.jobs.checks.morning;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.juror.job.execution.client.contracts.SchedulerServiceClient;
import uk.gov.hmcts.juror.job.execution.jobs.LinearJob;
import uk.gov.hmcts.juror.job.execution.model.Status;
import uk.gov.hmcts.juror.job.execution.rules.Rules;
import uk.gov.hmcts.juror.job.execution.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
@Getter
public class DashboardMorningChecksJob extends LinearJob {

    private final DashboardMorningChecksConfig config;
    private final Clock clock;

    private final SchedulerServiceClient schedulerServiceClient;
    private final ObjectMapper objectMapper;


    @Autowired
    protected DashboardMorningChecksJob(DashboardMorningChecksConfig config,
                                        SchedulerServiceClient schedulerServiceClient,
                                        ObjectMapper objectMapper,
                                        Clock clock) {
        super();
        this.config = config;
        this.clock = clock;
        this.objectMapper = objectMapper;
        this.schedulerServiceClient = schedulerServiceClient;
        addRules(
            Rules.requireDirectory(this.config.getArchiveFolder())
        );
    }

    @Override
    public ResultSupplier getResultSupplier() {
        final Support support = new Support();
        return new ResultSupplier(false,
            List.of(
                metaData -> archivePreviousCheckFile(support),
                metaData -> checkScheduledJobsRan(support)
            )
        );
    }

    Result archivePreviousCheckFile(final Support support) {
        FileUtils.move(this.config.getAttachmentFile(),
            new File(this.config.getArchiveFolder().getAbsolutePath()
                + "/Juror_MorningChecks_" + support.getLogDateTimeStr() + ".csv"), true);
        return Result.passed();
    }

    @SuppressWarnings({"PMD.LawOfDemeter", "PMD.CognitiveComplexity"})
    Result checkScheduledJobsRan(Support support) {
        try {
            Map<String, String> metaData = new ConcurrentHashMap<>();

            MorningChecksJsonConfig morningChecksConfig =
                this.objectMapper.readValue(this.config.getExpectedJobConfigLocation(),
                    MorningChecksJsonConfig.class);
            MorningChecksJsonConfig.DayConfig dayConfig = morningChecksConfig.getDay(support.getDayOfWeek());

            Status resultStatus = Status.SUCCESS;
            for (String jobKey : dayConfig.getExpectedJobs()) {

                SchedulerServiceClient.TaskResponse taskResponse =
                    this.schedulerServiceClient.getLatestTask(jobKey);

                Support.TableItem.Status status;
                if (taskResponse != null) {
                    if (taskResponse.getCreatedAt().isBefore(support.getStartTime().minusHours(24))) {
                        status = Support.TableItem.Status.JOB_NOT_FOUND_EXECUTED;
                        metaData.put(jobKey, "was not executed in last 24 hours");
                        support.addMessage(jobKey + " was not executed in last 24 hours");
                    } else {
                        status = Support.TableItem.mapStatus(taskResponse.getStatus());
                        metaData.put(jobKey, taskResponse.getStatus().name());
                        if (taskResponse.getStatus() != Status.SUCCESS) {
                            support.addMessage(jobKey + " was not successful");
                        }
                    }
                } else {
                    status = Support.TableItem.Status.JOB_NOT_FOUND;
                    metaData.put(jobKey, "was not found");
                    support.addMessage(jobKey + " was not found");
                }
                support.addTableRow("Scheduled Job", jobKey, status);
                if (status != Support.TableItem.Status.OK) {
                    resultStatus = Status.FAILED;
                }
            }


            String message;
            if (resultStatus == Status.SUCCESS) {
                message = "All expected jobs executed successfully today";
            } else {
                message = "One or more checks failed";
            }
            return new Result(resultStatus,
                message,
                null)
                .addMetaData(metaData);
        } catch (Exception e) {
            log.error("Failed to check jobs result", e);
            return new Result(Status.FAILED_UNEXPECTED_EXCEPTION, "Failed to check jobs result", e);
        }
    }

    @Getter
    public class Support {
        private final String logDateTimeStr;
        private static final String PAGE_START = "<!DOCTYPE html><html lang='en'><head><meta http-equiv='Content-Type' "
            + "content='text/html; "
            + "charset=iso-8859-1'></head><body><table border=0 cellpadding=2 width='70%' cellspacing=0 "
            + "width=50% style='{border: 1px solid #000000;}'>";

        private static final String PAGE_END = "</body></html>";
        private static final String TABLE_START =
            "<tr><td width='20%' bgcolor='Silver' style='{border-right: 1px solid #000000 ;}'>&nbsp</td><td "
                + "bgcolor='Silver'style='{border-right: 1px solid #000000;}'><b><font size=2 "
                + "face='Caliri'>Morning Check Details</font></b></td><td width='10%' bgcolor='Silver'><b><font "
                + "size=2 face='Caliri'>Result</font></b></td></tr>";
        private static final String TABLE_END = "</table><br>";

        private final List<String> messages;
        private final List<TableItem> tableItems;

        private static final DateTimeFormatter LOG_DATE_TIME_FORMATTER;

        static {
            LOG_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("ddMMyyyy_HHmmss");
        }

        private final DayOfWeek dayOfWeek;
        private final LocalDateTime startTime;

        public Support() {
            this.messages = new ArrayList<>();
            this.tableItems = new ArrayList<>();
            this.startTime = LocalDateTime.now(clock);
            this.logDateTimeStr = startTime.format(LOG_DATE_TIME_FORMATTER);
            this.dayOfWeek = DayOfWeek.from(this.startTime);
        }

        public void addMessage(String message) {
            this.messages.add(message);
        }

        public void addTableRow(String title, String message, TableItem.Status status) {
            this.tableItems.add(new TableItem(title, message, status));
        }

        void saveToFile(String htmlResponse) throws IOException {
            FileUtils.writeToFile(config.getAttachmentFile(), htmlResponse);
        }

        public String buildHtml() {
            StringBuilder builder = new StringBuilder(22);
            builder.append(PAGE_START)
                .append(TABLE_START);
            for (TableItem tableItem : this.tableItems) {
                builder.append(tableItem.toHtml());
            }
            builder.append(TABLE_END)
                .append("<br><ul>");
            for (String message : this.messages) {
                builder.append("<li>").append(message).append("</li>");
            }
            builder.append("</ul>")
                .append(PAGE_END);
            return builder.toString();
        }

        @Getter
        @AllArgsConstructor
        public static class TableItem {
            private static final String STYLE = "style='{border-right: 1px solid #000000 ;}'";
            private static final String FONT = "<font size=2 face='Calibri'>";
            private String title;
            private String message;
            private Status status;

            @SuppressWarnings("PMD.UnnecessaryFullyQualifiedName")//False positive
            public static Status mapStatus(uk.gov.hmcts.juror.job.execution.model.Status status) {
                if (status == uk.gov.hmcts.juror.job.execution.model.Status.SUCCESS
                    || status == uk.gov.hmcts.juror.job.execution.model.Status.VALIDATION_PASSED
                ) {
                    return Status.OK;
                } else {
                    return Status.NOT_OK;
                }
            }

            @Getter
            public enum Status {
                OK("OK", "92d050"),
                NOT_OK("NOT OK", "RED"),
                JOB_NOT_FOUND("JOB NOT FOUND", "RED"),
                JOB_NOT_FOUND_EXECUTED("JOB NOT EXECUTED", "RED");

                private final String colour;
                private final String displayName;

                Status(String displayName, String colour) {
                    this.displayName = displayName;
                    this.colour = colour;
                }

            }


            public String toHtml() {
                return "<tr>"
                    + "<td width='20%' " + STYLE + ">" + FONT
                    + title
                    + "</font></td><td " + STYLE + ">" + FONT
                    + message
                    + "</font></td><td width='10%' align='centre' bgcolor='"
                    + getStatus().getColour()
                    + "'>" + status.getDisplayName() + "</td></tr>";
            }
        }
    }
}
