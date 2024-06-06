package uk.gov.hmcts.juror.job.execution.jobs.dashboard.ams.data;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import uk.gov.hmcts.juror.job.execution.client.contracts.SchedulerServiceClient;
import uk.gov.hmcts.juror.job.execution.jobs.Job;
import uk.gov.hmcts.juror.job.execution.model.Status;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AutoSysTest {

    private SchedulerServiceClient schedulerServiceClient;
    private Clock clock;
    private DashboardData dashboardData;
    private AutoSys autoSys;

    @BeforeEach
    void beforeEach() {
        this.clock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
        this.schedulerServiceClient = mock(SchedulerServiceClient.class);
        this.dashboardData = mock(DashboardData.class);
        this.autoSys = spy(new AutoSys(dashboardData, schedulerServiceClient, clock));
    }

    @Test
    void positiveConstructorTest() {
        assertSame(dashboardData, autoSys.dashboardData);
        assertSame(schedulerServiceClient, autoSys.schedulerServiceClient);
        assertSame(clock, autoSys.clock);

        assertEquals("AUTOSYS LOG", autoSys.title,
            "Expected title to be AUTOSYS LOG");
        assertEquals(2, autoSys.columCount,
            "Expected column count to be 2");
        assertEquals(2, autoSys.rows.get(0).length);
        assertEquals("Job", autoSys.rows.get(0)[0]);
        assertEquals("Status", autoSys.rows.get(0)[1]);
    }

    @Test
    void positiveAddRowTest() {
        doNothing().when(autoSys).addEntry("job", "status");
        autoSys.addRow("job", "status");
        verify(autoSys, times(1)).addEntry("job", "status");
    }

    @Test
    void positivePopulateAllPass() {
        when(dashboardData.getErrorsOvernight()).thenReturn(mock(ErrorsOvernight.class));
        SchedulerServiceClient.TaskResponse taskResponse = mock(SchedulerServiceClient.TaskResponse.class);
        when(taskResponse.getLastUpdatedAt()).thenReturn(LocalDateTime.now(clock).minusHours(1));
        when(taskResponse.getStatus()).thenReturn(Status.SUCCESS);

        LocalDateTime lastUpdatedAt = LocalDateTime.now();
        SchedulerServiceClient.TaskResponse lastUpdatedTaskResponse = mock(SchedulerServiceClient.TaskResponse.class);
        when(lastUpdatedTaskResponse.getLastUpdatedAt()).thenReturn(lastUpdatedAt);
        when(lastUpdatedTaskResponse.getStatus()).thenReturn(Status.SUCCESS);

        doReturn(taskResponse).when(autoSys).populate("CONFIRM_LETTER");
        doReturn(taskResponse).when(autoSys).populate("PAYMENT_JOB");
        doReturn(taskResponse).when(autoSys).populate("PRINT_JOB");
        doReturn(lastUpdatedTaskResponse).when(autoSys).populate("POOL_TRANSFER");
        doReturn(taskResponse).when(autoSys).populate("WITHDRAW_LETTER");
        doReturn(taskResponse).when(autoSys).populate("PNC_CHECK_BULK");

        doNothing().when(autoSys).populateTimestamp(any(), any(), any(LocalDateTime.class));

        Job.Result result = autoSys.populate();
        assertEquals(Job.Result.passed(), result, "Expected result to be passed");


        verify(autoSys, times(1)).populateTimestamp(dashboardData, "AUTOSYS LOG", lastUpdatedAt);
        verify(autoSys, times(1)).populateTimestamp(dashboardData, "Errors Overnight", lastUpdatedAt);
        verify(dashboardData, times(1)).getErrorsOvernight();
        verify(dashboardData.getErrorsOvernight(), times(1))
            .addRow("SSUPVL03", "None");

        verify(autoSys, times(1)).populate("PAYMENT_JOB");
        verify(autoSys, times(1)).populate("PRINT_JOB");
        verify(autoSys, times(1)).populate("POOL_TRANSFER");
        verify(autoSys, times(1)).populate("PNC_CHECK_BULK");
        verify(autoSys, times(1)).populate();
    }

    @Test
    void negativePopulateAllFoundButNotAllSuccess() {
        when(dashboardData.getErrorsOvernight()).thenReturn(mock(ErrorsOvernight.class));
        SchedulerServiceClient.TaskResponse taskResponse = mock(SchedulerServiceClient.TaskResponse.class);
        when(taskResponse.getLastUpdatedAt()).thenReturn(LocalDateTime.now().minusHours(1));
        when(taskResponse.getStatus()).thenReturn(Status.SUCCESS);

        LocalDateTime lastUpdatedAt = LocalDateTime.now();
        SchedulerServiceClient.TaskResponse lastUpdatedTaskResponse = mock(SchedulerServiceClient.TaskResponse.class);
        when(lastUpdatedTaskResponse.getLastUpdatedAt()).thenReturn(lastUpdatedAt);
        when(lastUpdatedTaskResponse.getStatus()).thenReturn(Status.SUCCESS);

        SchedulerServiceClient.TaskResponse faildeTaskResponse = mock(SchedulerServiceClient.TaskResponse.class);
        when(faildeTaskResponse.getLastUpdatedAt()).thenReturn(LocalDateTime.now().minusHours(1));
        when(faildeTaskResponse.getStatus()).thenReturn(Status.FAILED);

        doReturn(taskResponse).when(autoSys).populate("CONFIRM_LETTER");
        doReturn(taskResponse).when(autoSys).populate("PAYMENT_JOB");
        doReturn(faildeTaskResponse).when(autoSys).populate("PRINT_JOB");
        doReturn(lastUpdatedTaskResponse).when(autoSys).populate("POOL_TRANSFER");
        doReturn(taskResponse).when(autoSys).populate("WITHDRAW_LETTER");
        doReturn(taskResponse).when(autoSys).populate("PNC_CHECK_BULK");

        doNothing().when(autoSys).populateTimestamp(any(), any(), any(LocalDateTime.class));

        Job.Result result = autoSys.populate();

        assertEquals(Job.Result.failed("One or more jobs failed"), result,
            "Expected result to be failed");

        verify(autoSys, times(1)).populateTimestamp(dashboardData, "AUTOSYS LOG", lastUpdatedAt);
        verify(autoSys, times(1)).populateTimestamp(dashboardData, "Errors Overnight", lastUpdatedAt);
        verify(dashboardData, times(1)).getErrorsOvernight();
        verify(dashboardData.getErrorsOvernight(), times(1))
            .addRow("SSUPVL03", "ERROR");

        verify(autoSys, times(1)).populate("PAYMENT_JOB");
        verify(autoSys, times(1)).populate("PRINT_JOB");
        verify(autoSys, times(1)).populate("POOL_TRANSFER");
        verify(autoSys, times(1)).populate("PNC_CHECK_BULK");
        verify(autoSys, times(1)).populate();
    }

    @Test
    void negativePopulateMissingTaskEntity() {
        when(dashboardData.getErrorsOvernight()).thenReturn(mock(ErrorsOvernight.class));
        SchedulerServiceClient.TaskResponse taskResponse = mock(SchedulerServiceClient.TaskResponse.class);
        when(taskResponse.getLastUpdatedAt()).thenReturn(LocalDateTime.now().minusHours(1));
        when(taskResponse.getStatus()).thenReturn(Status.SUCCESS);

        LocalDateTime lastUpdatedAt = LocalDateTime.now();
        SchedulerServiceClient.TaskResponse lastUpdatedTaskResponse = mock(SchedulerServiceClient.TaskResponse.class);
        when(lastUpdatedTaskResponse.getLastUpdatedAt()).thenReturn(lastUpdatedAt);
        when(lastUpdatedTaskResponse.getStatus()).thenReturn(Status.SUCCESS);

        doReturn(taskResponse).when(autoSys).populate("PAYMENT_JOB");
        doReturn(null).when(autoSys).populate("PRINT_JOB");
        doReturn(lastUpdatedTaskResponse).when(autoSys).populate("POOL_TRANSFER");
        doReturn(taskResponse).when(autoSys).populate("PNC_CHECK_BULK");

        doNothing().when(autoSys).populateTimestamp(any(), any(), any(LocalDateTime.class));

        Job.Result result = autoSys.populate();
        assertEquals(Job.Result.failed("One or more jobs failed"), result,
            "Expected result to be failed");

        verify(autoSys, times(1)).populateTimestamp(dashboardData, "AUTOSYS LOG", lastUpdatedAt);
        verify(autoSys, times(1)).populateTimestamp(dashboardData, "Errors Overnight", lastUpdatedAt);
        verify(dashboardData, times(1)).getErrorsOvernight();
        verify(dashboardData.getErrorsOvernight(), times(1))
            .addRow("SSUPVL03", "ERROR");

        verify(autoSys, times(1)).populate("PAYMENT_JOB");
        verify(autoSys, times(1)).populate("PRINT_JOB");
        verify(autoSys, times(1)).populate("POOL_TRANSFER");
        verify(autoSys, times(1)).populate("PNC_CHECK_BULK");
        verify(autoSys, times(1)).populate();

    }


    @ParameterizedTest
    @EnumSource(value = Status.class)
    void positivePopulateJobKeyFound(Status status) {
        SchedulerServiceClient.TaskResponse expectedTaskResponse = mock(SchedulerServiceClient.TaskResponse.class);
        when(expectedTaskResponse.getStatus()).thenReturn(status);
        when(expectedTaskResponse.getCreatedAt()).thenReturn(LocalDateTime.now(clock).minusHours(1));
        when(schedulerServiceClient.getLatestTask("CONFIRM_LETTER")).thenReturn(expectedTaskResponse);
        doNothing().when(autoSys).addRow(any(), any());

        SchedulerServiceClient.TaskResponse actualTaskResponse = autoSys.populate("CONFIRM_LETTER");
        assertSame(expectedTaskResponse, actualTaskResponse, "Expected and actual task response to be the same");
        verify(autoSys, times(1)).addRow("CONFIRM_LETTER", status.name());
    }

    @Test
    void negativePopulateJobKeyNotFound() {
        when(schedulerServiceClient.getLatestTask("CONFIRM_LETTER")).thenReturn(null);
        doNothing().when(autoSys).addRow(any(), any());

        assertNull(autoSys.populate("CONFIRM_LETTER"), "Task response should be null");
        verify(autoSys, times(1)).addRow("CONFIRM_LETTER", "ERROR");
    }

    @Test
    void negativePopulateJobKeyFoundButCreatedMoreThen24HoursAgo() {
        SchedulerServiceClient.TaskResponse expectedTaskResponse = mock(SchedulerServiceClient.TaskResponse.class);
        when(expectedTaskResponse.getStatus()).thenReturn(Status.SUCCESS);
        when(expectedTaskResponse.getCreatedAt()).thenReturn(LocalDateTime.now(clock).minusHours(25));
        when(schedulerServiceClient.getLatestTask("CONFIRM_LETTER")).thenReturn(expectedTaskResponse);
        doNothing().when(autoSys).addRow(any(), any());

        SchedulerServiceClient.TaskResponse actualTaskResponse = autoSys.populate("CONFIRM_LETTER");
        assertSame(expectedTaskResponse, actualTaskResponse, "Expected and actual task response to be the same");
        verify(autoSys, times(1)).addRow("CONFIRM_LETTER", "ERROR");
    }

    @Test
    void negativePopulateJobKeyUnexpectedException() {
        RuntimeException cause = new RuntimeException("I am the cause");
        doThrow(cause).when(schedulerServiceClient).getLatestTask("CONFIRM_LETTER");
        doNothing().when(autoSys).addRow(any(), any());

        assertNull(autoSys.populate("CONFIRM_LETTER"), "Task response should be null");
        verify(autoSys, times(1)).addRow("CONFIRM_LETTER", "ERROR");
    }
}
