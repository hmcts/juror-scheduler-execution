package uk.gov.hmcts.juror.job.execution.jobs.dashboard.ams.data;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.juror.job.execution.client.contracts.SchedulerServiceClient;
import uk.gov.hmcts.juror.job.execution.jobs.Job;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class BureauLettersAutomaticallyGeneratedTest {


    private SchedulerServiceClient schedulerServiceClient;

    private BureauLettersAutomaticallyGenerated bureauLettersAutomaticallyGenerated;
    private DashboardData dashboardData;

    @BeforeEach
    void beforeEach() {
        this.schedulerServiceClient = mock(SchedulerServiceClient.class);
        this.dashboardData = mock(DashboardData.class);
        this.bureauLettersAutomaticallyGenerated =
            spy(new BureauLettersAutomaticallyGenerated(dashboardData, schedulerServiceClient));
    }

    @Test
    void positiveConstructorTest() {
        assertSame(dashboardData, bureauLettersAutomaticallyGenerated.dashboardData);
        assertSame(schedulerServiceClient, bureauLettersAutomaticallyGenerated.schedulerServiceClient);
        assertEquals("Bureau Letters Automatically Generated", bureauLettersAutomaticallyGenerated.title,

            "Expected title to be Bureau Letters Automatically Generated");
        assertEquals(2, bureauLettersAutomaticallyGenerated.columCount,
            "Expected column count to be 2");
        assertEquals(2, bureauLettersAutomaticallyGenerated.rows.get(0).length);
        assertEquals("Type", bureauLettersAutomaticallyGenerated.rows.get(0)[0]);
        assertEquals("Count", bureauLettersAutomaticallyGenerated.rows.get(0)[1]);
    }

    @Test
    void positiveAddRow() {
        doNothing().when(bureauLettersAutomaticallyGenerated).addEntry("someType", "someCount");
        bureauLettersAutomaticallyGenerated.addRow("someType", "someCount");
        verify(bureauLettersAutomaticallyGenerated, times(1)).addEntry("someType", "someCount");
    }

    @Test
    void positivePopulateFirstIsNewest() {
        LocalDateTime confirmationLetterLastUpdatedAt = LocalDateTime.now();
        LocalDateTime withdrawLetterLastUpdatedAt = confirmationLetterLastUpdatedAt.minusSeconds(1);

        doReturn(confirmationLetterLastUpdatedAt).when(bureauLettersAutomaticallyGenerated)
            .addBureauLettersAutomaticallyGeneratedValue("CONFIRMATION",
                "CONFIRM_LETTER");
        doReturn(withdrawLetterLastUpdatedAt).when(bureauLettersAutomaticallyGenerated)
            .addBureauLettersAutomaticallyGeneratedValue("WITHDRAW_LETTER",
                "WITHDRAW_LETTER");

        doNothing().when(bureauLettersAutomaticallyGenerated).populateTimestamp(eq(dashboardData), any(),
            any(LocalDateTime.class));

        assertEquals(Job.Result.passed(), bureauLettersAutomaticallyGenerated.populate(),
            "Expected result success to be returned");


        verify(bureauLettersAutomaticallyGenerated, times(1)).addBureauLettersAutomaticallyGeneratedValue(
            "CONFIRMATION", "CONFIRM_LETTER");
        verify(bureauLettersAutomaticallyGenerated, times(1)).addBureauLettersAutomaticallyGeneratedValue(
            "WITHDRAW_LETTER", "WITHDRAW_LETTER");
        verify(bureauLettersAutomaticallyGenerated, times(1)).populateTimestamp(dashboardData,
            "Bureau Letters Automatically Generated", confirmationLetterLastUpdatedAt);
    }

    @Test
    void positivePopulateSecondIsNewest() {
        LocalDateTime confirmationLetterLastUpdatedAt = LocalDateTime.now();
        LocalDateTime withdrawLetterLastUpdatedAt = confirmationLetterLastUpdatedAt.plusSeconds(1);

        doReturn(confirmationLetterLastUpdatedAt).when(bureauLettersAutomaticallyGenerated)
            .addBureauLettersAutomaticallyGeneratedValue("CONFIRMATION",
                "CONFIRM_LETTER");
        doReturn(withdrawLetterLastUpdatedAt).when(bureauLettersAutomaticallyGenerated)
            .addBureauLettersAutomaticallyGeneratedValue("WITHDRAW_LETTER",
                "WITHDRAW_LETTER");

        doNothing().when(bureauLettersAutomaticallyGenerated)
            .populateTimestamp(eq(dashboardData), any(), any(LocalDateTime.class));

        assertEquals(Job.Result.passed(), bureauLettersAutomaticallyGenerated.populate(),
            "Expected result success to be returned");


        verify(bureauLettersAutomaticallyGenerated, times(1)).addBureauLettersAutomaticallyGeneratedValue(
            "CONFIRMATION", "CONFIRM_LETTER");
        verify(bureauLettersAutomaticallyGenerated, times(1)).addBureauLettersAutomaticallyGeneratedValue(
            "WITHDRAW_LETTER", "WITHDRAW_LETTER");
        verify(bureauLettersAutomaticallyGenerated, times(1)).populateTimestamp(dashboardData,
            "Bureau Letters Automatically Generated", withdrawLetterLastUpdatedAt);
    }

    @Test
    void positivePopulateConfirmationNull() {
        LocalDateTime withdrawLetterLastUpdatedAt = LocalDateTime.now();

        doReturn(null).when(bureauLettersAutomaticallyGenerated)
            .addBureauLettersAutomaticallyGeneratedValue("CONFIRMATION",
                "CONFIRM_LETTER");
        doReturn(withdrawLetterLastUpdatedAt).when(bureauLettersAutomaticallyGenerated)
            .addBureauLettersAutomaticallyGeneratedValue("WITHDRAW_LETTER",
                "WITHDRAW_LETTER");

        doNothing().when(bureauLettersAutomaticallyGenerated)
            .populateTimestamp(eq(dashboardData), any(), any(LocalDateTime.class));

        assertEquals(Job.Result.failed("Failed to get Confirmation letter or withdraw letter information"),
            bureauLettersAutomaticallyGenerated.populate(),
            "Expected result success to be returned");

        verify(bureauLettersAutomaticallyGenerated, times(1)).addBureauLettersAutomaticallyGeneratedValue(
            "CONFIRMATION", "CONFIRM_LETTER");
        verify(bureauLettersAutomaticallyGenerated, times(1)).addBureauLettersAutomaticallyGeneratedValue(
            "WITHDRAW_LETTER", "WITHDRAW_LETTER");
        verify(bureauLettersAutomaticallyGenerated, times(1)).populateTimestamp(dashboardData,
            "Bureau Letters Automatically Generated", withdrawLetterLastUpdatedAt);
    }

    @Test
    void positivePopulateWithdrawNull() {
        LocalDateTime confirmationLetterLastUpdatedAt = LocalDateTime.now();

        doReturn(confirmationLetterLastUpdatedAt).when(bureauLettersAutomaticallyGenerated)
            .addBureauLettersAutomaticallyGeneratedValue("CONFIRMATION",
                "CONFIRM_LETTER");
        doReturn(null).when(bureauLettersAutomaticallyGenerated)
            .addBureauLettersAutomaticallyGeneratedValue("WITHDRAW_LETTER",
                "WITHDRAW_LETTER");

        doNothing().when(bureauLettersAutomaticallyGenerated)
            .populateTimestamp(eq(dashboardData), any(), any(LocalDateTime.class));

        assertEquals(Job.Result.failed("Failed to get Confirmation letter or withdraw letter information"),
            bureauLettersAutomaticallyGenerated.populate(),
            "Expected result success to be returned");

        verify(bureauLettersAutomaticallyGenerated, times(1)).addBureauLettersAutomaticallyGeneratedValue(
            "CONFIRMATION", "CONFIRM_LETTER");
        verify(bureauLettersAutomaticallyGenerated, times(1)).addBureauLettersAutomaticallyGeneratedValue(
            "WITHDRAW_LETTER", "WITHDRAW_LETTER");
        verify(bureauLettersAutomaticallyGenerated, times(1)).populateTimestamp(dashboardData,
            "Bureau Letters Automatically Generated", confirmationLetterLastUpdatedAt);
    }

    @Test
    void positivePopulateBothNull() {
        doReturn(null).when(bureauLettersAutomaticallyGenerated)
            .addBureauLettersAutomaticallyGeneratedValue("CONFIRMATION",
                "CONFIRM_LETTER");
        doReturn(null).when(bureauLettersAutomaticallyGenerated)
            .addBureauLettersAutomaticallyGeneratedValue("WITHDRAW_LETTER",
                "WITHDRAW_LETTER");

        doNothing().when(bureauLettersAutomaticallyGenerated)
            .populateTimestamp(eq(dashboardData), any(), any(LocalDateTime.class));

        assertEquals(Job.Result.failed("Failed to get Confirmation letter or withdraw letter information"),
            bureauLettersAutomaticallyGenerated.populate(),
            "Expected result success to be returned");

        verify(bureauLettersAutomaticallyGenerated, times(1)).addBureauLettersAutomaticallyGeneratedValue(
            "CONFIRMATION", "CONFIRM_LETTER");
        verify(bureauLettersAutomaticallyGenerated, times(1)).addBureauLettersAutomaticallyGeneratedValue(
            "WITHDRAW_LETTER", "WITHDRAW_LETTER");
        verify(bureauLettersAutomaticallyGenerated, times(1)).populateTimestamp(dashboardData,
            "Bureau Letters Automatically Generated", LocalDateTime.MIN);
    }

    @Test
    void positiveAddBureauLettersAutomaticallyGeneratedValue() {
        LocalDateTime lastUpdatedAt = LocalDateTime.now();

        SchedulerServiceClient.TaskResponse taskResponse = mock(SchedulerServiceClient.TaskResponse.class);
        when(schedulerServiceClient.getLatestTask("CONFIRM_LETTER"))
            .thenReturn(taskResponse);
        when(taskResponse.getLastUpdatedAt()).thenReturn(lastUpdatedAt);
        when(taskResponse.getMetaData()).thenReturn(Map.of(
            "LETTERS_AUTOMATICALLY_GENERATED", "1"
        ));
        doNothing().when(bureauLettersAutomaticallyGenerated).addRow(any(), any());

        assertSame(lastUpdatedAt, bureauLettersAutomaticallyGenerated.addBureauLettersAutomaticallyGeneratedValue(
                "CONFIRMATION", "CONFIRM_LETTER"),
            "Expected lastUpdatedAt to be returned");

        verify(schedulerServiceClient, times(1)).getLatestTask("CONFIRM_LETTER");
        verify(taskResponse, times(1)).getLastUpdatedAt();
        verify(taskResponse, times(1)).getMetaData();
        verify(bureauLettersAutomaticallyGenerated, times(1)).addRow("CONFIRMATION", "1");

        verify(bureauLettersAutomaticallyGenerated, times(1))
            .addBureauLettersAutomaticallyGeneratedValue("CONFIRMATION", "CONFIRM_LETTER");
        verifyNoMoreInteractions(bureauLettersAutomaticallyGenerated);
    }

    @Test
    void negativeAddBureauLettersAutomaticallyGeneratedValueNullTaskResponse() {

        when(schedulerServiceClient.getLatestTask("CONFIRM_LETTER"))
            .thenReturn(null);
        doNothing().when(bureauLettersAutomaticallyGenerated).addRow(any(), any());

        assertNull(bureauLettersAutomaticallyGenerated.addBureauLettersAutomaticallyGeneratedValue(
                "CONFIRMATION", "CONFIRM_LETTER"),
            "Expected null to be returned");

        verify(schedulerServiceClient, times(1)).getLatestTask("CONFIRM_LETTER");
        verify(bureauLettersAutomaticallyGenerated, times(1))
            .addRow("CONFIRMATION", "ERROR");

        verify(bureauLettersAutomaticallyGenerated, times(1))
            .addBureauLettersAutomaticallyGeneratedValue("CONFIRMATION", "CONFIRM_LETTER");
        verifyNoMoreInteractions(bureauLettersAutomaticallyGenerated);

    }

    @Test
    void negativeAddBureauLettersAutomaticallyGeneratedValueTaskResponseMissingLettersGeneratedMetaData() {
        LocalDateTime lastUpdatedAt = LocalDateTime.now();

        SchedulerServiceClient.TaskResponse taskResponse = mock(SchedulerServiceClient.TaskResponse.class);
        when(schedulerServiceClient.getLatestTask("CONFIRM_LETTER"))
            .thenReturn(taskResponse);
        when(taskResponse.getLastUpdatedAt()).thenReturn(lastUpdatedAt);
        doNothing().when(bureauLettersAutomaticallyGenerated).addRow(any(), any());

        assertSame(lastUpdatedAt, bureauLettersAutomaticallyGenerated.addBureauLettersAutomaticallyGeneratedValue(
                "CONFIRMATION", "CONFIRM_LETTER"),
            "Expected lastUpdatedAt to be returned");

        verify(schedulerServiceClient, times(1)).getLatestTask("CONFIRM_LETTER");
        verify(taskResponse, times(1)).getLastUpdatedAt();
        verify(taskResponse, times(1)).getMetaData();
        verify(bureauLettersAutomaticallyGenerated, times(1)).addRow("CONFIRMATION", "ERROR");

        verify(bureauLettersAutomaticallyGenerated, times(1))
            .addBureauLettersAutomaticallyGeneratedValue("CONFIRMATION", "CONFIRM_LETTER");
        verifyNoMoreInteractions(bureauLettersAutomaticallyGenerated);

    }

    @Test
    void negativeAddBureauLettersAutomaticallyGeneratedValueUnexpectedException() {
        RuntimeException cause = new RuntimeException("I am the cause");
        when(schedulerServiceClient.getLatestTask("CONFIRM_LETTER"))
            .thenThrow(cause);
        doNothing().when(bureauLettersAutomaticallyGenerated).addRow(any(), any());

        assertNull(bureauLettersAutomaticallyGenerated.addBureauLettersAutomaticallyGeneratedValue(
                "CONFIRMATION", "CONFIRM_LETTER"),
            "Expected null to be returned");

        verify(schedulerServiceClient, times(1)).getLatestTask("CONFIRM_LETTER");
        verify(bureauLettersAutomaticallyGenerated, times(1))
            .addRow("CONFIRMATION", "ERROR");

        verify(bureauLettersAutomaticallyGenerated, times(1))
            .addBureauLettersAutomaticallyGeneratedValue("CONFIRMATION", "CONFIRM_LETTER");
        verifyNoMoreInteractions(bureauLettersAutomaticallyGenerated);
    }

}
