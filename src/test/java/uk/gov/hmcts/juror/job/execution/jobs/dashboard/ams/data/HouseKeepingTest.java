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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class HouseKeepingTest {


    private HouseKeeping houseKeeping;
    private DashboardData dashboardData;
    private SchedulerServiceClient schedulerServiceClient;
    private Clock clock;

    @BeforeEach
    void beforeEach() {
        this.dashboardData = mock(DashboardData.class);
        this.schedulerServiceClient = mock(SchedulerServiceClient.class);
        this.clock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
        this.houseKeeping = spy(new HouseKeeping(dashboardData, schedulerServiceClient, clock));
    }

    @Test
    void positiveConstructorTest() {
        assertSame(dashboardData, houseKeeping.dashboardData,
            "DashboardData should be the same");
        assertSame(schedulerServiceClient, houseKeeping.schedulerServiceClient,
            "schedulerServiceClient should be the same");
        assertSame(clock, houseKeeping.clock,
            "Clock should be the same");

        assertEquals("Housekeeping", houseKeeping.title,
            "Expected title to be Housekeeping");
        assertEquals(2, houseKeeping.columCount,
            "Expected column count to be 2");
        assertEquals(2, houseKeeping.rows.get(0).length,
            "Expected row size to be 2");
        assertEquals("Status", houseKeeping.rows.get(0)[0],
            "Expected row 0 column 0 to be Status");
        assertEquals("Other Info", houseKeeping.rows.get(0)[1],
            "Expected row 0 column 1 to be Other Info");
    }

    @Test
    void positiveAddRowTest() {
        assertEquals(1, houseKeeping.rows.size());
        houseKeeping.addRow("someStatus", "someOtherInformation");
        assertEquals(2, houseKeeping.rows.size());
        assertEquals(2, houseKeeping.rows.get(1).length);
        assertEquals("someStatus", houseKeeping.rows.get(1)[0],
            "Expected row 1 column 0 to be someStatus");
        assertEquals("someOtherInformation", houseKeeping.rows.get(1)[1],
            "Expected row 1 column 1 to be someOtherInformation");

    }

    @Test
    void positivePopulateTest() {
        SchedulerServiceClient.TaskResponse taskResponse = mock(SchedulerServiceClient.TaskResponse.class);
        when(taskResponse.getCreatedAt()).thenReturn(LocalDateTime.now());
        when(taskResponse.getLastUpdatedAt()).thenReturn(LocalDateTime.now());
        when(taskResponse.getStatus()).thenReturn(Status.SUCCESS);

        when(schedulerServiceClient.getLatestTask("HOUSE_KEEPING"))
            .thenReturn(taskResponse);

        doNothing().when(houseKeeping)
            .populateTimestamp(any(), any(), any(LocalDateTime.class));
        assertEquals(Job.Result.passed(), houseKeeping.populate(),
            "Expected result to be passed");


        verify(schedulerServiceClient, times(1)).getLatestTask("HOUSE_KEEPING");
        verify(taskResponse, times(2)).getCreatedAt();
        verify(taskResponse, times(1)).getLastUpdatedAt();
        verify(taskResponse, times(1)).getStatus();
        verify(houseKeeping, times(1)).addRow("Success", "");

        verify(houseKeeping, times(1))
            .populateTimestamp(dashboardData, "Housekeeping", taskResponse.getLastUpdatedAt());

        verifyNoMoreInteractions(dashboardData, schedulerServiceClient);
    }

    @Test
    void negativePopulateNullResponseTest() {
        when(schedulerServiceClient.getLatestTask("HOUSE_KEEPING"))
            .thenReturn(null);

        doNothing().when(houseKeeping)
            .populateTimestamp(any(), any(), any(String.class));
        assertEquals(Job.Result.failed("House keeping job not found"), houseKeeping.populate(),
            "Expected result to be failed with message");


        verify(schedulerServiceClient, times(1)).getLatestTask("HOUSE_KEEPING");
        verify(houseKeeping, times(1)).addRow("ERROR", "House keeping Job not found");

        verify(houseKeeping, times(1))
            .populateTimestamp(dashboardData, "Housekeeping", "ERROR");

        verifyNoMoreInteractions(dashboardData, schedulerServiceClient);
    }

    @Test
    void negativePopulateNullCreatedAtResponseTest() {
        SchedulerServiceClient.TaskResponse taskResponse = mock(SchedulerServiceClient.TaskResponse.class);
        when(taskResponse.getCreatedAt()).thenReturn(null);
        when(schedulerServiceClient.getLatestTask("HOUSE_KEEPING"))
            .thenReturn(taskResponse);

        doNothing().when(houseKeeping)
            .populateTimestamp(any(), any(), any(String.class));
        assertEquals(Job.Result.failed("House keeping job not found"), houseKeeping.populate(),
            "Expected result to be failed with message");


        verify(schedulerServiceClient, times(1)).getLatestTask("HOUSE_KEEPING");
        verify(houseKeeping, times(1)).addRow("ERROR", "House keeping Job not found");

        verify(houseKeeping, times(1))
            .populateTimestamp(dashboardData, "Housekeeping", "ERROR");

        verifyNoMoreInteractions(dashboardData, schedulerServiceClient);
    }

    @Test
    void negativePopulateCreatedMoreThan7DaysAgoResponseTest() {
        SchedulerServiceClient.TaskResponse taskResponse = mock(SchedulerServiceClient.TaskResponse.class);
        when(taskResponse.getCreatedAt()).thenReturn(LocalDateTime.now(clock).minusDays(8));
        when(taskResponse.getLastUpdatedAt()).thenReturn(LocalDateTime.now());
        when(taskResponse.getStatus()).thenReturn(Status.SUCCESS);
        when(schedulerServiceClient.getLatestTask("HOUSE_KEEPING"))
            .thenReturn(taskResponse);

        doNothing().when(houseKeeping)
            .populateTimestamp(any(), any(), any(LocalDateTime.class));
        assertEquals(Job.Result.failed("Last run was run more then 7 days ago"), houseKeeping.populate(),
            "Expected result to be failed with message");


        verify(schedulerServiceClient, times(1)).getLatestTask("HOUSE_KEEPING");
        verify(houseKeeping, times(1)).addRow("Failed", "Last run was run more then 7 days ago");

        verify(houseKeeping, times(1))
            .populateTimestamp(dashboardData, "Housekeeping", taskResponse.getLastUpdatedAt());

        verifyNoMoreInteractions(dashboardData, schedulerServiceClient);
    }

    @ParameterizedTest
    @EnumSource(value = Status.class, mode = EnumSource.Mode.EXCLUDE, names = {"SUCCESS"})
    void negativePopulateResponseNotSuccessResponseTest(Status status) {
        SchedulerServiceClient.TaskResponse taskResponse = mock(SchedulerServiceClient.TaskResponse.class);
        when(taskResponse.getCreatedAt()).thenReturn(LocalDateTime.now(clock));
        when(taskResponse.getLastUpdatedAt()).thenReturn(LocalDateTime.now());
        when(taskResponse.getStatus()).thenReturn(status);
        when(schedulerServiceClient.getLatestTask("HOUSE_KEEPING"))
            .thenReturn(taskResponse);

        doNothing().when(houseKeeping)
            .populateTimestamp(any(), any(), any(LocalDateTime.class));
        assertEquals(Job.Result.failed("Expected status 'SUCCESS' but was " + status.name()), houseKeeping.populate(),
            "Expected result to be failed with message");


        verify(schedulerServiceClient, times(1)).getLatestTask("HOUSE_KEEPING");
        verify(houseKeeping, times(1)).addRow("Failed", "Status: " + status);

        verify(houseKeeping, times(1))
            .populateTimestamp(dashboardData, "Housekeeping", taskResponse.getLastUpdatedAt());

        verifyNoMoreInteractions(dashboardData, schedulerServiceClient);
    }

    @Test
    void negativePopulateUnexpectedExceptionTest() {
        RuntimeException cause = new RuntimeException("I am the cause");

        when(schedulerServiceClient.getLatestTask("HOUSE_KEEPING"))
            .thenThrow(cause);

        doNothing().when(houseKeeping)
            .populateTimestamp(any(), any(), any(String.class));
        assertEquals(
            Job.Result.failed("Unexpected gateway exception when getting latest task for HOUSE_KEEPING", cause),
            houseKeeping.populate(),
            "Expected result to be failed with message");


        verify(schedulerServiceClient, times(1)).getLatestTask("HOUSE_KEEPING");
        verify(houseKeeping, times(1)).addRow("Failed", "Unexpected exception");

        verify(houseKeeping, times(1))
            .populateTimestamp(dashboardData, "Housekeeping", "ERROR");

        verifyNoMoreInteractions(dashboardData, schedulerServiceClient);

    }
}
