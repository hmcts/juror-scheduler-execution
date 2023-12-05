package uk.gov.hmcts.juror.job.execution.jobs.checks.morning;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import uk.gov.hmcts.juror.job.execution.client.contracts.SchedulerServiceClient;
import uk.gov.hmcts.juror.job.execution.database.model.MetaData;
import uk.gov.hmcts.juror.job.execution.jobs.Job;
import uk.gov.hmcts.juror.job.execution.model.Status;
import uk.gov.hmcts.juror.job.execution.rules.Rule;
import uk.gov.hmcts.juror.job.execution.rules.Rules;
import uk.gov.hmcts.juror.job.execution.service.contracts.SmtpService;
import uk.gov.hmcts.juror.job.execution.util.FileUtils;
import uk.gov.hmcts.juror.standard.service.exceptions.InternalServerException;

import java.io.File;
import java.io.IOException;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class DashboardMorningChecksJobTest {
    private DashboardMorningChecksConfig config;
    private Clock clock;
    private SchedulerServiceClient schedulerServiceClient;
    private ObjectMapper objectMapper;
    private SmtpService smtpService;
    private DashboardMorningChecksJob dashboardMorningChecksJob;

    private MockedStatic<FileUtils> fileUtilsMock;

    @BeforeEach
    void beforeEach() {
        clock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
        config = createConfig();
        schedulerServiceClient = mock(SchedulerServiceClient.class);
        objectMapper = mock(ObjectMapper.class);
        smtpService = mock(SmtpService.class);
        dashboardMorningChecksJob =
            new DashboardMorningChecksJob(config, schedulerServiceClient, smtpService, objectMapper, clock);
    }

    @AfterEach
    void afterEach() {
        if (fileUtilsMock != null) {
            fileUtilsMock.close();
        }
    }

    private DashboardMorningChecksConfig createConfig() {
        DashboardMorningChecksConfig config = new DashboardMorningChecksConfig();
        config.setArchiveFolder(mock(File.class));
        config.setAttachmentFile(mock(File.class));
        config.setEmailRecipients(new String[]{
            RandomStringUtils.randomAlphabetic(10),
            RandomStringUtils.randomAlphabetic(10),
            RandomStringUtils.randomAlphabetic(10)});
        return config;
    }

    @Test
    void positiveConstructorTest() {
        assertEquals(config, dashboardMorningChecksJob.getConfig(),
            "DashboardMorningChecksJob constructor should set config");
        assertEquals(schedulerServiceClient, dashboardMorningChecksJob.getSchedulerServiceClient(),
            "DashboardMorningChecksJob constructor should set schedulerServiceClient");
        assertEquals(objectMapper, dashboardMorningChecksJob.getObjectMapper(),
            "DashboardMorningChecksJob constructor should set objectMapper");
        assertEquals(smtpService, dashboardMorningChecksJob.getSmtpService(),
            "DashboardMorningChecksJob constructor should set smtpService");
        assertEquals(clock, dashboardMorningChecksJob.getClock(),
            "DashboardMorningChecksJob constructor should set clock");

        assertEquals(1, dashboardMorningChecksJob.getRules().size(),
            "DashboardMorningChecksJob constructor should set rules");

        Rule rule = dashboardMorningChecksJob.getRules().iterator().next();
        assertInstanceOf(Rules.RequireDirectoryRule.class, rule,
            "DashboardMorningChecksJob constructor should set rules");
        assertEquals(Rules.requireDirectory(config.getArchiveFolder()), rule,
            "DashboardMorningChecksJob constructor should set rules");
    }


    @Test
    void positiveGetResultSupplier() {
        fileUtilsMock = Mockito.mockStatic(FileUtils.class);
        dashboardMorningChecksJob = spy(dashboardMorningChecksJob);

        doReturn(Job.Result.passed("Message 1")).when(dashboardMorningChecksJob).archivePreviousCheckFile(any());
        doReturn(Job.Result.passed("Message 2")).when(dashboardMorningChecksJob).checkScheduledJobsRan(any());

        Job.ResultSupplier resultSupplier = dashboardMorningChecksJob.getResultSupplier();
        assertFalse(resultSupplier.isContinueOnFailure(), "ResultSupplier should not continue on failure");
        MetaData metaData = mock(MetaData.class);

        assertEquals(2, resultSupplier.getResultRunners().size(), "Result runners should be 2");

        Iterator<Function<MetaData, Job.Result>> resultRunnerIterator = resultSupplier.getResultRunners().iterator();

        Job.Result result1 = resultRunnerIterator.next().apply(metaData);
        assertEquals(Status.SUCCESS, result1.getStatus(), "Result should be success");
        assertEquals("Message 1", result1.getMessage(), "Result should match");

        Job.Result result2 = resultRunnerIterator.next().apply(metaData);
        assertEquals(Status.SUCCESS, result2.getStatus(), "Result should be success");
        assertEquals("Message 2", result2.getMessage(), "Result should match");


        resultSupplier.getPostRunChecks().accept(Job.Result.passed("Message 3"));

        final String expectedHttpResponse =
            "<!DOCTYPE html><html lang='en'><head><meta http-equiv='Content-Type' content='text/html; "
                + "charset=iso-8859-1'></head><body><table border=0 cellpadding=2 width='70%' cellspacing=0 width=50%"
                + " style='{border: 1px solid #000000;}'><tr><td width='20%' bgcolor='Silver' style='{border-right: "
                + "1px solid #000000 ;}'>&nbsp</td><td bgcolor='Silver'style='{border-right: 1px solid #000000;"
                + "}'><b><font size=2 face='Caliri'>Morning Check Details</font></b></td><td width='10%' "
                + "bgcolor='Silver'><b><font size=2 face='Caliri'>Result</font></b></td></tr></table><br><br><ul></ul"
                + "></body></html>";

        verify(smtpService, times(1)).sendEmail(config.getSmtp(),
            "JUROR Daily Checks: SUCCESS",
            expectedHttpResponse,
            config.getEmailRecipients());
        verify(dashboardMorningChecksJob).archivePreviousCheckFile(any());
        verify(dashboardMorningChecksJob).checkScheduledJobsRan(any());

        fileUtilsMock.verify(() -> FileUtils.writeToFile(config.getAttachmentFile(), expectedHttpResponse),
            times(1));
    }

    @Test
    void positiveArchivePreviousCheckFileTest() {
        String logDateTime = "SomeAccurateTime";
        fileUtilsMock = Mockito.mockStatic(FileUtils.class);
        DashboardMorningChecksJob.Support support = mock(DashboardMorningChecksJob.Support.class);
        when(support.getLogDateTimeStr()).thenReturn(logDateTime);

        Job.Result result = dashboardMorningChecksJob.archivePreviousCheckFile(support);

        fileUtilsMock.verify(() -> FileUtils.move(config.getAttachmentFile(),
                new File(this.config.getArchiveFolder().getAbsolutePath() +
                    "/Juror_MorningChecks_" + logDateTime +
                    ".csv"), true),
            times(1));
    }

    @Nested
    @DisplayName("Result checkScheduledJobsRan(Support support)")
    class CheckScheduledJobsRan {
        @Test
        void positiveNoExpectedJobsToday() throws IOException {
            callAndValidate(List.of());
        }

        void callAndValidate(List<SchedulerServiceClient.TaskResponse> taskResponseList) throws IOException {
            callAndValidate(taskResponseList, List.of());
        }

        void callAndValidate(List<SchedulerServiceClient.TaskResponse> taskResponseList,
                             List<String> notFoundKeys) throws IOException {
            DashboardMorningChecksJob.Support support = mock(DashboardMorningChecksJob.Support.class);
            when(support.getDayOfWeek()).thenReturn(DayOfWeek.MONDAY);
            when(support.getStartTime()).thenReturn(LocalDateTime.now(clock));

            LocalDateTime time24HoursAgo = LocalDateTime.now(clock).minusSeconds(86400);
            MorningChecksJsonConfig morningChecksJsonConfig = new MorningChecksJsonConfig();
            MorningChecksJsonConfig.DayConfig dayConfig = new MorningChecksJsonConfig.DayConfig();

            dayConfig.setExpectedJobs(
                new ArrayList<>(taskResponseList.stream().map(SchedulerServiceClient.TaskResponse::getJobKey)
                    .toList()));
            dayConfig.getExpectedJobs().addAll(notFoundKeys);

            morningChecksJsonConfig.setDays(Map.of(DayOfWeek.MONDAY, dayConfig));

            when(objectMapper.readValue(config.getExpectedJobConfigLocation(), MorningChecksJsonConfig.class))
                .thenReturn(morningChecksJsonConfig);


            for (SchedulerServiceClient.TaskResponse taskResponse : taskResponseList) {
                when(schedulerServiceClient.getLatestTask(taskResponse.getJobKey()))
                    .thenReturn(taskResponse);
            }
            for (String notFoundJobKey : notFoundKeys) {
                when(schedulerServiceClient.getLatestTask(notFoundJobKey))
                    .thenReturn(null);
            }
            Job.Result result = dashboardMorningChecksJob.checkScheduledJobsRan(support);


            if (taskResponseList.stream().anyMatch(taskResponse ->
                taskResponse.getStatus() != Status.SUCCESS
                    || taskResponse.getCreatedAt().isBefore(time24HoursAgo))
                || !notFoundKeys.isEmpty()) {
                assertEquals(Status.FAILED, result.getStatus(), "Result status should be failed");
                assertEquals("One or more checks failed", result.getMessage(),
                    "Result message should match");
            } else {
                assertEquals(Status.SUCCESS, result.getStatus(), "Result status should be success");
                assertEquals("All expected jobs executed successfully today", result.getMessage(),
                    "Result message should match");
            }

            assertEquals(taskResponseList.size() + notFoundKeys.size(), result.getMetaData().size(),
                "Result should have an entry for every job key");


            for (SchedulerServiceClient.TaskResponse taskResponse : taskResponseList) {
                assertTrue(result.getMetaData().containsKey(taskResponse.getJobKey()),
                    "Result should have an meta data entry for every job key");

                String expectedMessage;
                DashboardMorningChecksJob.Support.TableItem.Status expectedTableStatus;

                if (taskResponse.getCreatedAt()
                    .isBefore(time24HoursAgo)) {
                    expectedMessage = "was not executed in last 24 hours";
                    expectedTableStatus = DashboardMorningChecksJob.Support.TableItem.Status.JOB_NOT_FOUND_EXECUTED;

                    verify(support, times(1))
                        .addMessage(taskResponse.getJobKey() + " was not executed in last 24 hours");
                } else {
                    expectedMessage = taskResponse.getStatus().name();
                    expectedTableStatus = switch (taskResponse.getStatus()) {
                        case SUCCESS, VALIDATION_PASSED -> DashboardMorningChecksJob.Support.TableItem.Status.OK;
                        default -> DashboardMorningChecksJob.Support.TableItem.Status.NOT_OK;
                    };
                    if (taskResponse.getStatus() != Status.SUCCESS) {
                        verify(support, times(1))
                            .addMessage(taskResponse.getJobKey() + " was not successful");
                    }
                }

                assertEquals(expectedMessage, result.getMetaData().get(taskResponse.getJobKey()),
                    "Meta data value should match");
                verify(support, times(1))
                    .addTableRow("Scheduled Job", taskResponse.getJobKey(), expectedTableStatus);
            }

            for (String notFoundJobKey : notFoundKeys) {
                assertEquals("was not found", result.getMetaData().get(notFoundJobKey),
                    "Meta data value should match");

                verify(support, times(1))
                    .addTableRow("Scheduled Job", notFoundJobKey,
                        DashboardMorningChecksJob.Support.TableItem.Status.JOB_NOT_FOUND);
                verify(support, times(1))
                    .addMessage(notFoundJobKey + " was not found");

            }

            assertNull(result.getThrowable(), "Result should not have throwable");
            if (!taskResponseList.isEmpty()) {
                verify(support, times(taskResponseList.size())).getStartTime();
            }
            verify(support, times(1)).getDayOfWeek();
            verifyNoMoreInteractions(support);
        }

        @Test
        void positiveSingleJobToday() throws IOException {
            SchedulerServiceClient.TaskResponse taskResponse = spy(new SchedulerServiceClient.TaskResponse());
            taskResponse.setJobKey(RandomStringUtils.randomAlphabetic(10));
            taskResponse.setStatus(Status.SUCCESS);
            taskResponse.setCreatedAt(LocalDateTime.now(clock).minusHours(10));
            callAndValidate(List.of(taskResponse));
        }

        @Test
        void positiveMultipleJobToday() throws IOException {
            SchedulerServiceClient.TaskResponse taskResponse1 = spy(new SchedulerServiceClient.TaskResponse());
            taskResponse1.setJobKey(RandomStringUtils.randomAlphabetic(10));
            taskResponse1.setStatus(Status.SUCCESS);
            taskResponse1.setCreatedAt(LocalDateTime.now(clock).minusHours(10));

            SchedulerServiceClient.TaskResponse taskResponse2 = spy(new SchedulerServiceClient.TaskResponse());
            taskResponse2.setJobKey(RandomStringUtils.randomAlphabetic(12));
            taskResponse2.setStatus(Status.VALIDATION_PASSED);
            taskResponse2.setCreatedAt(LocalDateTime.now(clock).minusHours(10));

            SchedulerServiceClient.TaskResponse taskResponse3 = spy(new SchedulerServiceClient.TaskResponse());
            taskResponse3.setJobKey(RandomStringUtils.randomAlphabetic(10));
            taskResponse3.setStatus(Status.FAILED_UNEXPECTED_EXCEPTION);
            taskResponse3.setCreatedAt(LocalDateTime.now(clock).minusHours(13));

            callAndValidate(List.of(taskResponse1, taskResponse2, taskResponse3));

        }

        @Test
        void negativeTaskResponseNotFound() throws IOException {
            callAndValidate(List.of(), List.of(RandomStringUtils.randomAlphabetic(10)));
        }

        @Test
        void negativeTaskResponseNoWithinLast24Hours() throws IOException {
            SchedulerServiceClient.TaskResponse taskResponse = spy(new SchedulerServiceClient.TaskResponse());
            taskResponse.setJobKey(RandomStringUtils.randomAlphabetic(10));
            taskResponse.setStatus(Status.SUCCESS);
            taskResponse.setCreatedAt(LocalDateTime.now(clock).minusHours(24).minusSeconds(1));
            callAndValidate(List.of(taskResponse));
        }

        @Test
        void negativeTaskResponseStatusIsNotSuccess() throws IOException {
            SchedulerServiceClient.TaskResponse taskResponse = spy(new SchedulerServiceClient.TaskResponse());
            taskResponse.setJobKey(RandomStringUtils.randomAlphabetic(10));
            taskResponse.setStatus(Status.VALIDATION_FAILED);
            taskResponse.setCreatedAt(LocalDateTime.now(clock).minusHours(10));
            callAndValidate(List.of(taskResponse));
        }

        @Test
        void negativeUnexpectedException() throws IOException {
            RuntimeException cause = new RuntimeException("I am the cause");

            DashboardMorningChecksJob.Support support = mock(DashboardMorningChecksJob.Support.class);

            doThrow(cause).when(objectMapper).readValue(config.getExpectedJobConfigLocation(),
                MorningChecksJsonConfig.class);

            Job.Result result = dashboardMorningChecksJob.checkScheduledJobsRan(support);
            assertEquals(Status.FAILED_UNEXPECTED_EXCEPTION, result.getStatus(), "Result status should be failed");
            assertEquals("Failed to check jobs result", result.getMessage(), "Result message should match");
            assertSame(cause, result.getThrowable(), "Result should have throwable");
            assertEquals(0, result.getMetaData().size(), "Result should have no meta data");
        }
    }

    @Nested
    @DisplayName("Support")
    class SupportTest {
        DashboardMorningChecksJob.Support support;

        @BeforeEach
        void beforeEach() {

            TestDashboardMorningChecksJob testDashboardMorningChecksJob =
                new TestDashboardMorningChecksJob(config, schedulerServiceClient, smtpService, objectMapper, clock);
            support = testDashboardMorningChecksJob.getSupport();
            dashboardMorningChecksJob = testDashboardMorningChecksJob;
        }

        @Test
        void positiveConstructorTest() {
            assertEquals(0, support.getMessages().size(),
                "Support constructor should initialise messages");
            assertEquals(0, support.getTableItems().size(),
                "Support constructor should initialise tableItems");
            assertEquals(LocalDateTime.now(clock), support.getStartTime(),
                "Support constructor should initialise startTime");
            assertEquals(DayOfWeek.from(LocalDateTime.now(clock)), support.getDayOfWeek(),
                "Support constructor should initialise dayOfWeek");
            assertEquals(LocalDateTime.now(clock).format(DateTimeFormatter.ofPattern("ddMMyyyy_HHmmss")),
                support.getLogDateTimeStr(),
                "Support constructor should initialise logDateTimeStr");
        }


        @Test
        void positiveAddMessage() {
            assertEquals(0, support.getMessages().size(),
                "Support constructor should initialise messages");
            support.addMessage("My first message");
            assertEquals(1, support.getMessages().size(),
                "Support constructor should initialise messages");
            assertEquals("My first message", support.getMessages().get(0),
                "Support constructor should initialise messages");

            support.addMessage("My second message");
            assertEquals(2, support.getMessages().size(),
                "Support constructor should initialise messages");
            assertEquals("My first message", support.getMessages().get(0),
                "Support constructor should initialise messages");
            assertEquals("My second message", support.getMessages().get(1),
                "Support constructor should initialise messages");

            support.addMessage("My third message");
            assertEquals(3, support.getMessages().size(),
                "Support constructor should initialise messages");
            assertEquals("My first message", support.getMessages().get(0),
                "Support constructor should initialise messages");
            assertEquals("My second message", support.getMessages().get(1),
                "Support constructor should initialise messages");
            assertEquals("My third message", support.getMessages().get(2),
                "Support constructor should initialise messages");
        }

        @Test
        void positiveAddTableRow() {
            assertEquals(0, support.getTableItems().size(),
                "Support constructor should initialise tableItems");

            support.addTableRow("MyTitle", "MyMessage", DashboardMorningChecksJob.Support.TableItem.Status.OK);
            assertEquals(1, support.getTableItems().size(),
                "Table should contain 1 item");
            DashboardMorningChecksJob.Support.TableItem tableItem1 = support.getTableItems().get(0);
            assertEquals("MyTitle", tableItem1.getTitle(),
                "Title should match");
            assertEquals("MyMessage", tableItem1.getMessage(),
                "Message should match");
            assertEquals(DashboardMorningChecksJob.Support.TableItem.Status.OK, tableItem1.getStatus(),
                "Status should match");

            support.addTableRow("My very good title", "Some other message",
                DashboardMorningChecksJob.Support.TableItem.Status.JOB_NOT_FOUND_EXECUTED);
            assertEquals(2, support.getTableItems().size(),
                "Table should contain 2 items");
            DashboardMorningChecksJob.Support.TableItem tableItem2 = support.getTableItems().get(1);
            assertEquals("My very good title", tableItem2.getTitle(),
                "Title should match");
            assertEquals("Some other message", tableItem2.getMessage(),
                "Message should match");
            assertEquals(DashboardMorningChecksJob.Support.TableItem.Status.JOB_NOT_FOUND_EXECUTED,
                tableItem2.getStatus(),
                "Status should match");
        }

        @Test
        void positiveSaveAndSendEmail() throws IOException {
            support = spy(support);
            Job.Result result = Job.Result.passed();
            String htmlResponse = "SomeHtmlResponse";
            doReturn(htmlResponse).when(support).buildHtml();
            doNothing().when(support).saveToFile(htmlResponse);
            doNothing().when(support).email(result, htmlResponse);


            support.saveToFileAndEmail(result);

            verify(support, times(1)).saveToFile(htmlResponse);
            verify(support, times(1)).email(result, htmlResponse);
        }

        @Test
        void negativeSaveAndSendEmailUnexpectedException() throws IOException {
            support = spy(support);
            Job.Result result = Job.Result.passed();
            String htmlResponse = "SomeHtmlResponse";
            RuntimeException cause = new RuntimeException("I am the cause");
            doReturn(htmlResponse).when(support).buildHtml();
            doThrow(cause).when(support).saveToFile(htmlResponse);

            InternalServerException exception = assertThrows(InternalServerException.class,
                () -> support.saveToFileAndEmail(result), "Should throw InternalServerException");

            assertEquals("Failed to save and email dashboard morning checks", exception.getMessage(),
                "Exception message should match");
            assertSame(cause, exception.getCause(), "Exception cause should match");
        }

        @Test
        void positiveSave() throws IOException {
            fileUtilsMock = Mockito.mockStatic(FileUtils.class);
            String htmlResponse = "Some Response";
            support.saveToFile(htmlResponse);
            fileUtilsMock.verify(() -> FileUtils.writeToFile(config.getAttachmentFile(), htmlResponse),
                times(1));
        }

        @Test
        void positiveEmail() {
            Job.Result result = Job.Result.passed();
            String htmlResponse = "Some Response";

            support.email(result, htmlResponse);

            verify(smtpService, times(1)).sendEmail(config.getSmtp(),
                "JUROR Daily Checks: SUCCESS",
                htmlResponse,
                config.getEmailRecipients());
        }

        @Test
        void positiveBuildHtml() {
            support.addTableRow("MyTitle", "MyMessage", DashboardMorningChecksJob.Support.TableItem.Status.OK);
            support.addTableRow("New Title", "New Message", DashboardMorningChecksJob.Support.TableItem.Status.NOT_OK);
            support.addMessage("Some message");
            support.addMessage("Some other message");

            String response = support.buildHtml();
            assertEquals(
                "<!DOCTYPE html><html lang='en'><head><meta http-equiv='Content-Type' content='text/html; "
                    + "charset=iso-8859-1'></head><body><table border=0 cellpadding=2 width='70%' cellspacing=0 "
                    + "width=50% style='{border: 1px solid #000000;}'><tr><td width='20%' bgcolor='Silver' "
                    + "style='{border-right: 1px solid #000000 ;}'>&nbsp</td><td "
                    + "bgcolor='Silver'style='{border-right: 1px solid #000000;}'><b><font size=2 "
                    + "face='Caliri'>Morning Check Details</font></b></td><td width='10%' bgcolor='Silver'><b><font "
                    + "size=2 face='Caliri'>Result</font></b></td></tr><tr><td width='20%' style='{border-right: 1px "
                    + "solid #000000 ;}'><font size=2 face='Calibri'>MyTitle</font></td><td style='{border-right: 1px"
                    + " solid #000000 ;}'><font size=2 face='Calibri'>MyMessage</font></td><td width='10%' "
                    + "align='centre' bgcolor='92d050'>OK</td></tr><tr><td width='20%' style='{border-right: 1px "
                    + "solid #000000 ;}'><font size=2 face='Calibri'>New Title</font></td><td style='{border-right: "
                    + "1px solid #000000 ;}'><font size=2 face='Calibri'>New Message</font></td><td width='10%' "
                    + "align='centre' bgcolor='RED'>NOT OK</td></tr></table><br><br><ul><li>Some message</li><li>Some"
                    + " other message</li></ul></body></html>",
                response, "Response should match");
        }


        @Nested
        @DisplayName("Support")
        class TableItemTest {

            @ParameterizedTest
            @EnumSource(value = Status.class, names = {"SUCCESS", "VALIDATION_PASSED"})
            void positiveMapStatusOK(Status status) {
                assertEquals(DashboardMorningChecksJob.Support.TableItem.Status.OK,
                    DashboardMorningChecksJob.Support.TableItem.mapStatus(status),
                    "Status should match");
            }

            @ParameterizedTest
            @EnumSource(value = Status.class,
                mode = EnumSource.Mode.EXCLUDE,
                names = {"SUCCESS", "VALIDATION_PASSED"})
            void positiveMapStatusNotOK(Status status) {
                assertEquals(DashboardMorningChecksJob.Support.TableItem.Status.NOT_OK,
                    DashboardMorningChecksJob.Support.TableItem.mapStatus(status),
                    "Status should match");
            }

            @Test
            void positiveStatusConstructorTest() {
                DashboardMorningChecksJob.Support.TableItem tableItem = new DashboardMorningChecksJob.Support.TableItem(
                    "SomeTitle", "SomeMessage", DashboardMorningChecksJob.Support.TableItem.Status.OK);

                assertEquals("SomeTitle", tableItem.getTitle(), "Title should match");
                assertEquals("SomeMessage", tableItem.getMessage(), "Message should match");
                assertEquals(DashboardMorningChecksJob.Support.TableItem.Status.OK, tableItem.getStatus(),
                    "Status should match");

            }

            @Test
            void positiveToHtml() {
                DashboardMorningChecksJob.Support.TableItem tableItem = new DashboardMorningChecksJob.Support.TableItem(
                    "SomeTitle", "SomeMessage", DashboardMorningChecksJob.Support.TableItem.Status.OK);
                String expectedHtml =
                    "<tr><td width='20%' style='{border-right: 1px solid #000000 ;}'><font size=2 "
                        + "face='Calibri'>SomeTitle</font></td><td style='{border-right: 1px solid #000000 ;}'><font "
                        + "size=2 face='Calibri'>SomeMessage</font></td><td width='10%' align='centre' "
                        + "bgcolor='92d050'>OK</td></tr>";
                assertEquals(expectedHtml, tableItem.toHtml(),
                    "Html should match");
            }
        }

        class TestDashboardMorningChecksJob extends DashboardMorningChecksJob {

            protected TestDashboardMorningChecksJob(DashboardMorningChecksConfig config,
                                                    SchedulerServiceClient schedulerServiceClient,
                                                    SmtpService smtpService,
                                                    ObjectMapper objectMapper, Clock clock) {
                super(config, schedulerServiceClient, smtpService, objectMapper, clock);
            }

            public Support getSupport() {
                return new Support();
            }
        }
    }
}
