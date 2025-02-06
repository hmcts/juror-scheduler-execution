package uk.gov.hmcts.juror.job.execution.jobs.dashboard.ams.data;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.juror.job.execution.client.contracts.SchedulerServiceClient;
import uk.gov.hmcts.juror.job.execution.jobs.Job;
import uk.gov.hmcts.juror.job.execution.jobs.checks.pnc.batch.PoliceCheck;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PncCheckTest {

    private DashboardData dashboardData;
    private SchedulerServiceClient schedulerServiceClient;
    private PncCheck pncCheck;

    @BeforeEach
    void beforeEach() {
        this.dashboardData = mock(DashboardData.class);
        this.schedulerServiceClient = mock(SchedulerServiceClient.class);
        this.pncCheck = spy(new PncCheck(dashboardData, schedulerServiceClient));
    }

    @Test
    void positiveConstructorTest() {
        assertSame(dashboardData, pncCheck.dashboardData,
            "DashboardData should be the same");
        assertSame(schedulerServiceClient, pncCheck.schedulerServiceClient,
            "schedulerServiceClient should be the same");

        assertEquals("PNC Checks", pncCheck.title,
            "Expected title to be PNC Checks");

        assertEquals(5, pncCheck.columCount,
            "Expected column count to be 5");
        assertEquals(1, pncCheck.rows.size(),
            "Expected rows size to be 1");
        assertEquals(5, pncCheck.rows.get(0).length,
            "Expected row size to be 5");

        assertEquals("Type", pncCheck.rows.get(0)[0],
            "Expected row 0 column 0 to be Type");
        assertEquals("Submitted", pncCheck.rows.get(0)[1],
            "Expected row 0 column 1 to be Submitted");
        assertEquals("Completed", pncCheck.rows.get(0)[2],
            "Expected row 0 column 2 to be Completed");
        assertEquals("Failed", pncCheck.rows.get(0)[3],
            "Expected row 0 column 3 to be Failed");
        assertEquals("Unchecked", pncCheck.rows.get(0)[4],
            "Expected row 0 column 4 to be Unchecked");
    }

    @Test
    void positiveAddRowTest() {
        assertEquals(1, pncCheck.rows.size(),
            "Expected rows size to be 1");

        pncCheck.addRow("SomeType", "submittedValue", "completedValue", "failedValue", "uncheckedValue");
        assertEquals(2, pncCheck.rows.size(),
            "Expected rows size to be 1");
        assertEquals(5, pncCheck.rows.get(1).length,
            "Expected row size to be 5");
        assertEquals("SomeType", pncCheck.rows.get(1)[0],
            "Expected row 1 column 0 to be SomeType");
        assertEquals("submittedValue", pncCheck.rows.get(1)[1],
            "Expected row 1 column 1 to be submittedValue");
        assertEquals("completedValue", pncCheck.rows.get(1)[2],
            "Expected row 1 column 2 to be completedValue");
        assertEquals("failedValue", pncCheck.rows.get(1)[3],
            "Expected row 1 column 3 to be failedValue");
        assertEquals("uncheckedValue", pncCheck.rows.get(1)[4],
            "Expected row 1 column 4 to be uncheckedValue");
    }

    @Test
    void positivePopulateTest() {
        Map<String, String> metaData = new ConcurrentHashMap<>();
        metaData.put("TOTAL_CHECKS_REQUESTED", "10");
        metaData.put("TOTAL_WITH_STATUS_ELIGIBLE", "5");
        metaData.put("TOTAL_WITH_STATUS_INELIGIBLE", "5");
        metaData.put("TOTAL_WITH_STATUS_UNCHECKED_MAX_RETRIES_EXCEEDED", "0");

        SchedulerServiceClient.TaskResponse taskResponse = mock(SchedulerServiceClient.TaskResponse.class);
        when(schedulerServiceClient.getLatestTask("PNC_BATCH")).thenReturn(taskResponse);
        when(taskResponse.getMetaData()).thenReturn(metaData);
        when(taskResponse.getLastUpdatedAt()).thenReturn(LocalDateTime.now());

        doNothing().when(pncCheck).populateTimestamp(any(), any(), any(LocalDateTime.class));
        assertEquals(Job.Result.passed(), pncCheck.populate(),
            "Expected result to be passed");

        verify(pncCheck, times(1))
            .addRow("Bureau", "10", "10", "5", "0");

        verify(pncCheck, times(1))
            .populateTimestamp(dashboardData, "PNC Checks", taskResponse.getLastUpdatedAt());

        verify(pncCheck, times(1)).populate();
    }

    @Test
    void positivePopulate2Test() {
        Map<String, String> metaData = new ConcurrentHashMap<>();
        metaData.put("TOTAL_CHECKS_REQUESTED", "10");
        metaData.put("TOTAL_WITH_STATUS_ELIGIBLE", "4");
        metaData.put("TOTAL_WITH_STATUS_INELIGIBLE", "4");
        metaData.put("TOTAL_WITH_STATUS_UNCHECKED_MAX_RETRIES_EXCEEDED", "2");

        SchedulerServiceClient.TaskResponse taskResponse = mock(SchedulerServiceClient.TaskResponse.class);
        when(schedulerServiceClient.getLatestTask("PNC_BATCH")).thenReturn(taskResponse);
        when(taskResponse.getMetaData()).thenReturn(metaData);
        when(taskResponse.getLastUpdatedAt()).thenReturn(LocalDateTime.now());

        doNothing().when(pncCheck).populateTimestamp(any(), any(), any(LocalDateTime.class));
        assertEquals(Job.Result.passed(), pncCheck.populate(),
            "Expected result to be passed");

        verify(pncCheck, times(1))
            .addRow("Bureau", "10", "8", "4", "2");

        verify(pncCheck, times(1))
            .populateTimestamp(dashboardData, "PNC Checks", taskResponse.getLastUpdatedAt());

        verify(pncCheck, times(1)).populate();
    }

    @Test
    void positivePopulate3Test() {
        Map<String, String> metaData = new ConcurrentHashMap<>();
        metaData.put("TOTAL_CHECKS_REQUESTED", "12");
        metaData.put("TOTAL_WITH_STATUS_ELIGIBLE", "4");
        metaData.put("TOTAL_WITH_STATUS_INELIGIBLE", "4");
        metaData.put("TOTAL_WITH_STATUS_UNCHECKED_MAX_RETRIES_EXCEEDED", "2");

        SchedulerServiceClient.TaskResponse taskResponse = mock(SchedulerServiceClient.TaskResponse.class);
        when(schedulerServiceClient.getLatestTask("PNC_BATCH")).thenReturn(taskResponse);
        when(taskResponse.getMetaData()).thenReturn(metaData);
        when(taskResponse.getLastUpdatedAt()).thenReturn(LocalDateTime.now());

        doNothing().when(pncCheck).populateTimestamp(any(), any(), any(LocalDateTime.class));
        assertEquals(Job.Result.passed(), pncCheck.populate(),
            "Expected result to be passed");

        verify(pncCheck, times(1))
            .addRow("Bureau", "12", "8", "4", "2");

        verify(pncCheck, times(1))
            .populateTimestamp(dashboardData, "PNC Checks", taskResponse.getLastUpdatedAt());

        verify(pncCheck, times(1)).populate();
    }

    @Test
    void negativePopulateTaskResponseNullTest() {
        when(schedulerServiceClient.getLatestTask("PNC_BATCH")).thenReturn(null);

        when(dashboardData.getTimestamps()).thenReturn(mock(Timestamps.class));
        assertEquals(Job.Result.failed("Failed to get PNC check results"), pncCheck.populate(),
            "Expected result to be failed with message");

        verify(pncCheck, times(1))
            .addRow("Bureau", "ERROR", "ERROR", "ERROR", "ERROR");

        verify(pncCheck, times(1))
            .populateTimestamp(dashboardData, "PNC Checks", (LocalDateTime) null);

        verify(pncCheck, times(1)).populate();
    }

    @Test
    void negativePopulateUnexpectedExceptionTest() {
        RuntimeException cause = new RuntimeException("I am the cause");
        when(schedulerServiceClient.getLatestTask("PNC_BATCH")).thenThrow(cause);

        when(dashboardData.getTimestamps()).thenReturn(mock(Timestamps.class));
        assertEquals(Job.Result.failed("Failed to get PNC check results", cause), pncCheck.populate(),
            "Expected result to be failed with message");

        verify(pncCheck, times(1))
            .addRow("Bureau", "ERROR", "ERROR", "ERROR", "ERROR");

        verify(pncCheck, times(1))
            .populateTimestamp(dashboardData, "PNC Checks", (LocalDateTime) null);

        verify(pncCheck, times(1)).populate();
    }

    @Test
    void positiveGetPoliceCheckValueFoundTest() {
        Map<String, String> metaData = new ConcurrentHashMap<>();
        SchedulerServiceClient.TaskResponse taskResponse = mock(SchedulerServiceClient.TaskResponse.class);
        when(taskResponse.getMetaData()).thenReturn(metaData);
        metaData.put("TOTAL_WITH_STATUS_ELIGIBLE", "5");

        assertEquals(5, pncCheck.getPoliceCheckValue(taskResponse, PoliceCheck.ELIGIBLE),
            "Expected value to be 5");
    }

    @Test
    void positiveGetPoliceCheckValueNotFoundTest() {
        Map<String, String> metaData = new ConcurrentHashMap<>();
        SchedulerServiceClient.TaskResponse taskResponse = mock(SchedulerServiceClient.TaskResponse.class);
        when(taskResponse.getMetaData()).thenReturn(metaData);
        metaData.put("TOTAL_WITH_STATUS_ELIGIBLE", "5");

        assertEquals(0, pncCheck.getPoliceCheckValue(taskResponse, PoliceCheck.INELIGIBLE),
            "Expected value to be 0");
    }
}
