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

class BureauLettersToBePrintedTest {

    private DatabaseService databaseService;
    private DatabaseConfig databaseConfig;
    private Clock clock;
    private BureauLettersToBePrinted bureauLettersToBePrinted;
    private DashboardData dashboardData;

    @BeforeEach
    void beforeEach() {
        this.databaseService = mock(DatabaseService.class);
        this.databaseConfig = mock(DatabaseConfig.class);
        this.clock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
        this.dashboardData = mock(DashboardData.class);
        this.bureauLettersToBePrinted =
            spy(new BureauLettersToBePrinted(dashboardData, databaseService, databaseConfig, clock));
    }

    @Test
    void positiveConstructorTest() {
        assertSame(databaseService, bureauLettersToBePrinted.databaseService,
            "Expected databaseService to be set");
        assertSame(databaseConfig, bureauLettersToBePrinted.databaseConfig,
            "Expected databaseConfig to be set");
        assertSame(clock, bureauLettersToBePrinted.clock, "Expected clock to be set");

        assertEquals("Bureau Letters To Be Printed", bureauLettersToBePrinted.title,
            "Expected title to be Bureau Letters Automatically Generated");
        assertEquals(3, bureauLettersToBePrinted.columCount,
            "Expected column count to be 3");
        assertEquals(3, bureauLettersToBePrinted.rows.get(0).length);
        assertEquals("Type", bureauLettersToBePrinted.rows.get(0)[0]);
        assertEquals("Description", bureauLettersToBePrinted.rows.get(0)[1]);
        assertEquals("Count", bureauLettersToBePrinted.rows.get(0)[2]);
    }

    @Test
    void positiveAddRowTest() {
        doNothing().when(bureauLettersToBePrinted)
            .addEntry("someType", "someDescription", "someCount");

        bureauLettersToBePrinted.addRow("someType", "someDescription", "someCount");

        verify(bureauLettersToBePrinted)
            .addEntry("someType", "someDescription", "someCount");
    }

    @Test
    void positiveAddRowDBTest() {
        BureauLettersToBePrintedDB bureauLettersToBePrintedDB = new BureauLettersToBePrintedDB();
        bureauLettersToBePrintedDB.setType("someType");
        bureauLettersToBePrintedDB.setDescription("someDescription");
        bureauLettersToBePrintedDB.setCount(1L);

        doNothing().when(bureauLettersToBePrinted)
            .addEntry("someType", "someDescription", "someCount");

        bureauLettersToBePrinted.addRow(bureauLettersToBePrintedDB);

        verify(bureauLettersToBePrinted, times(1))
            .addEntry("someType", "someDescription", "1");

    }

    @Test
    void positivePopulate() {
        Connection connection = mock(Connection.class);
        doAnswer(invocation -> {
            Consumer<Connection> connectionConsumer = invocation.getArgument(1);
            connectionConsumer.accept(connection);
            return null;
        }).when(databaseService).execute(eq(databaseConfig), any());

        doNothing().when(bureauLettersToBePrinted).addRow(any());
        doNothing().when(bureauLettersToBePrinted).populateTimestamp(any(), any(), any(LocalDateTime.class));
        List<BureauLettersToBePrintedDB> responses = List.of(
            mock(BureauLettersToBePrintedDB.class),
            mock(BureauLettersToBePrintedDB.class),
            mock(BureauLettersToBePrintedDB.class)
        );

        when(databaseService.executePreparedStatement(connection, BureauLettersToBePrintedDB.class,
            BureauLettersToBePrinted.BUREAU_LETTERS_TO_BE_PRINTED_SQL))
            .thenReturn(responses);

        assertEquals(Job.Result.passed(),
            bureauLettersToBePrinted.populate(),
            "Expected result to be passed");

        for (BureauLettersToBePrintedDB bureauLettersToBePrintedDB : responses) {
            verify(bureauLettersToBePrinted, times(1))
                .addRow(bureauLettersToBePrintedDB);
        }

        verify(bureauLettersToBePrinted, times(1))
            .populateTimestamp(dashboardData, "Bureau Letters To Be Printed", LocalDateTime.now(clock));
    }

    @Test
    void negativePopulateEmptyResponse() {
        Connection connection = mock(Connection.class);
        doAnswer(invocation -> {
            Consumer<Connection> connectionConsumer = invocation.getArgument(1);
            connectionConsumer.accept(connection);
            return null;
        }).when(databaseService).execute(eq(databaseConfig), any());

        doNothing().when(bureauLettersToBePrinted).addRow(any());
        doNothing().when(bureauLettersToBePrinted).populateTimestamp(any(), any(), any(LocalDateTime.class));
        List<BureauLettersToBePrintedDB> responses = List.of();

        when(databaseService.executePreparedStatement(connection, BureauLettersToBePrintedDB.class,
            BureauLettersToBePrinted.BUREAU_LETTERS_TO_BE_PRINTED_SQL))
            .thenReturn(responses);

        assertEquals(Job.Result.failed("No response from database"),
            bureauLettersToBePrinted.populate(),
            "Expected result to be passed");

        verify(bureauLettersToBePrinted, times(1))
            .addRow("ERROR", "ERROR", "ERROR");


        verify(bureauLettersToBePrinted, times(1))
            .populateTimestamp(dashboardData, "Bureau Letters To Be Printed", LocalDateTime.now(clock));

    }

    @Test
    void negativePopulateNullResponse() {
        Connection connection = mock(Connection.class);
        doAnswer(invocation -> {
            Consumer<Connection> connectionConsumer = invocation.getArgument(1);
            connectionConsumer.accept(connection);
            return null;
        }).when(databaseService).execute(eq(databaseConfig), any());

        doNothing().when(bureauLettersToBePrinted).addRow(any());
        doNothing().when(bureauLettersToBePrinted).populateTimestamp(any(), any(), any(LocalDateTime.class));

        when(databaseService.executePreparedStatement(connection, BureauLettersToBePrintedDB.class,
            BureauLettersToBePrinted.BUREAU_LETTERS_TO_BE_PRINTED_SQL))
            .thenReturn(null);

        assertEquals(Job.Result.failed("No response from database"),
            bureauLettersToBePrinted.populate(),
            "Expected result to be passed");

        verify(bureauLettersToBePrinted, times(1))
            .addRow("ERROR", "ERROR", "ERROR");


        verify(bureauLettersToBePrinted, times(1))
            .populateTimestamp(dashboardData, "Bureau Letters To Be Printed", LocalDateTime.now(clock));

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

        doNothing().when(bureauLettersToBePrinted).addRow(any());
        doNothing().when(bureauLettersToBePrinted).populateTimestamp(any(), any(), any(LocalDateTime.class));

        when(databaseService.executePreparedStatement(connection, BureauLettersToBePrintedDB.class,
            BureauLettersToBePrinted.BUREAU_LETTERS_TO_BE_PRINTED_SQL))
            .thenThrow(cause);

        assertEquals(Job.Result.failed("Unexpected exception", cause),
            bureauLettersToBePrinted.populate(),
            "Expected result to be passed");

        verify(bureauLettersToBePrinted, times(1))
            .addRow("ERROR", "ERROR", "ERROR");

        verify(bureauLettersToBePrinted, times(1))
            .populateTimestamp(dashboardData, "Bureau Letters To Be Printed", LocalDateTime.now(clock));
    }
}
