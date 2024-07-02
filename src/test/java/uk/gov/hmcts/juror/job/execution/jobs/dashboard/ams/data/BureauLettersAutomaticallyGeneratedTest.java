package uk.gov.hmcts.juror.job.execution.jobs.dashboard.ams.data;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.juror.job.execution.config.DatabaseConfig;
import uk.gov.hmcts.juror.job.execution.jobs.Job;
import uk.gov.hmcts.juror.job.execution.service.contracts.DatabaseService;

import java.sql.Connection;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BureauLettersAutomaticallyGeneratedTest {
    private DatabaseService databaseService;
    private DatabaseConfig databaseConfig;
    private Clock clock;
    private BureauLettersAutomaticallyGenerated bureauLettersAutomaticallyGenerated;
    private DashboardData dashboardData;

    @BeforeEach
    void beforeEach() {
        this.databaseService = mock(DatabaseService.class);
        this.databaseConfig = mock(DatabaseConfig.class);
        this.clock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
        this.dashboardData = mock(DashboardData.class);
        this.bureauLettersAutomaticallyGenerated =
            spy(new BureauLettersAutomaticallyGenerated(dashboardData, databaseService, databaseConfig, clock));
    }

    @Test
    void positiveConstructorTest() {
        assertSame(dashboardData, bureauLettersAutomaticallyGenerated.dashboardData);
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
    void positiveAddBureauLettersAutomaticallyGeneratedValue() {
        Connection connection = mock(Connection.class);
        doAnswer(invocation -> {
            Consumer<Connection> connectionConsumer = invocation.getArgument(1);
            connectionConsumer.accept(connection);
            return null;
        }).when(databaseService).execute(eq(databaseConfig), any());

        doNothing().when(bureauLettersAutomaticallyGenerated).addRow(any(), any());
        doNothing().when(bureauLettersAutomaticallyGenerated)
            .populateTimestamp(eq(dashboardData), any(), any(LocalDateTime.class));
        List<BureauLettersAutomaticallyGeneratedDB> responses = List.of(
            mock(BureauLettersAutomaticallyGeneratedDB.class),
            mock(BureauLettersAutomaticallyGeneratedDB.class),
            mock(BureauLettersAutomaticallyGeneratedDB.class)
        );

        when(databaseService.executePreparedStatement(connection, BureauLettersAutomaticallyGeneratedDB.class,
            BureauLettersAutomaticallyGenerated.BUREAU_AUTO_GEN_LETTERS_SQL))
            .thenReturn(responses);

        assertEquals(Job.Result.passed(),
            bureauLettersAutomaticallyGenerated.populate(),
            "Expected result to be passed");

        for (BureauLettersAutomaticallyGeneratedDB bureauLettersAutomaticallyGeneratedDB : responses) {
            verify(bureauLettersAutomaticallyGenerated, times(1))
                .addRow(bureauLettersAutomaticallyGeneratedDB);
        }

        verify(bureauLettersAutomaticallyGenerated, times(1)).populateTimestamp(dashboardData,
            "Bureau Letters Automatically Generated", LocalDateTime.now(clock));
    }


    @Test
    void negativePopulateEmptyResponse() {
        Connection connection = mock(Connection.class);
        doAnswer(invocation -> {
            Consumer<Connection> connectionConsumer = invocation.getArgument(1);
            connectionConsumer.accept(connection);
            return null;
        }).when(databaseService).execute(eq(databaseConfig), any());

        doNothing().when(bureauLettersAutomaticallyGenerated).addRow(any());
        doNothing().when(bureauLettersAutomaticallyGenerated).populateTimestamp(any(), any(), any(LocalDateTime.class));
        List<BureauLettersAutomaticallyGeneratedDB> responses = List.of();

        when(databaseService.executePreparedStatement(connection, BureauLettersAutomaticallyGeneratedDB.class,
            bureauLettersAutomaticallyGenerated.BUREAU_AUTO_GEN_LETTERS_SQL))
            .thenReturn(responses);

        assertEquals(Job.Result.failed("No response from database"),
            bureauLettersAutomaticallyGenerated.populate(),
            "Expected result to be passed");

        verify(bureauLettersAutomaticallyGenerated, times(1))
            .addRow("Withdrawal", "ERROR");
        verify(bureauLettersAutomaticallyGenerated, times(1))
            .addRow("Confirmation", "ERROR");


        verify(bureauLettersAutomaticallyGenerated, times(1)).populateTimestamp(dashboardData,
            "Bureau Letters Automatically Generated", LocalDateTime.now(clock));

    }

    @Test
    void negativePopulateNullResponse() {
        Connection connection = mock(Connection.class);
        doAnswer(invocation -> {
            Consumer<Connection> connectionConsumer = invocation.getArgument(1);
            connectionConsumer.accept(connection);
            return null;
        }).when(databaseService).execute(eq(databaseConfig), any());

        doNothing().when(bureauLettersAutomaticallyGenerated).addRow(any());
        doNothing().when(bureauLettersAutomaticallyGenerated).populateTimestamp(any(), any(), any(LocalDateTime.class));

        when(databaseService.executePreparedStatement(connection, BureauLettersAutomaticallyGeneratedDB.class,
            bureauLettersAutomaticallyGenerated.BUREAU_AUTO_GEN_LETTERS_SQL))
            .thenReturn(null);

        Job.Result result = bureauLettersAutomaticallyGenerated.populate();

        assertEquals(Job.Result.failed("No response from database"),
            result,
            "Expected result to be failed");

        verify(bureauLettersAutomaticallyGenerated, times(1))
            .addRow("Withdrawal", "ERROR");
        verify(bureauLettersAutomaticallyGenerated, times(1))
            .addRow("Confirmation", "ERROR");

        verify(bureauLettersAutomaticallyGenerated, times(1)).populateTimestamp(dashboardData,
            "Bureau Letters Automatically Generated", LocalDateTime.now(clock));

    }

    @Test
    void negativePopulateUnexpectedException() {
        RuntimeException cause = new RuntimeException("I am the cause");
        Connection connection = mock(Connection.class);
        doAnswer(invocation -> {
            Consumer<Connection> connectionConsumer = invocation.getArgument(1);
            connectionConsumer.accept(connection);
            return null;
        }).when(databaseService).execute(eq(databaseConfig), any());

        doNothing().when(bureauLettersAutomaticallyGenerated).addRow(any());
        doNothing().when(bureauLettersAutomaticallyGenerated).populateTimestamp(any(), any(), any(LocalDateTime.class));

        when(databaseService.executePreparedStatement(connection, BureauLettersAutomaticallyGeneratedDB.class,
            BureauLettersAutomaticallyGenerated.BUREAU_AUTO_GEN_LETTERS_SQL))
            .thenThrow(cause);

        Job.Result result = bureauLettersAutomaticallyGenerated.populate();

        assertEquals(Job.Result.failed("Failed to get Confirmation letter or withdraw letter information"),
            result,
            "Expected result to be failed");

        verify(bureauLettersAutomaticallyGenerated, times(1))
            .addRow("Withdrawal", "ERROR");
        verify(bureauLettersAutomaticallyGenerated, times(1))
            .addRow("Confirmation", "ERROR");

        verify(bureauLettersAutomaticallyGenerated, times(0)).populateTimestamp(dashboardData,
            "Bureau Letters Automatically Generated", LocalDateTime.now(clock));
    }

}
