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
import java.time.ZoneOffset;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
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

class EmailsSentTest {
    private DatabaseService databaseService;
    private DatabaseConfig databaseConfig;
    private DashboardData dashboardData;
    private Clock clock;
    private EmailsSent emailsSent;
    private Connection connection;

    @BeforeEach
    void beforeEach() {
        databaseService = mock(DatabaseService.class);
        databaseConfig = mock(DatabaseConfig.class);
        dashboardData = mock(DashboardData.class);
        connection = mock(Connection.class);
        clock = Clock.fixed(Instant.parse("2026-11-24T06:00:00Z"), ZoneOffset.UTC);
        emailsSent = spy(new EmailsSent(dashboardData, databaseService, databaseConfig, clock));

        doAnswer(invocation -> {
            Consumer<Connection> connectionConsumer = invocation.getArgument(1);
            connectionConsumer.accept(connection);
            return null;
        }).when(databaseService).execute(eq(databaseConfig), any());
        doNothing().when(emailsSent).populateTimestamp(any(), any(), any(LocalDateTime.class));
    }

    @Test
    void positiveConstructorTest() {
        assertSame(databaseService, emailsSent.databaseService);
        assertSame(databaseConfig, emailsSent.databaseConfig);
        assertSame(clock, emailsSent.clock);
        assertEquals("Emails Sent", emailsSent.title);
        assertEquals(3, emailsSent.columCount);
        assertArrayEquals(new String[]{"Type", "Description", "Count"}, emailsSent.rows.get(0));
        assertEquals("""
            [Emails Sent]
            Type,Description,Count""", emailsSent.toCsv().toString());
    }

    @Test
    void positiveAddRowDbTest() {
        EmailsSentDB row = new EmailsSentDB()
            .setType("5221")
            .setDescription("SUMMONS ENGLISH")
            .setCount(4L);

        emailsSent.addRow(row);

        assertArrayEquals(new String[]{"5221", "SUMMONS ENGLISH", "4"}, emailsSent.rows.get(1));
    }

    @Test
    void positivePopulateUsesMondayWindowAndIncludesZeroCounts() {
        List<EmailsSentDB> response = List.of(
            new EmailsSentDB().setType("5221").setDescription("SUMMONS ENGLISH").setCount(12L),
            new EmailsSentDB().setType("5224AC").setDescription("CONFIRMATION WELSH").setCount(0L)
        );
        LocalDateTime start = LocalDateTime.of(2026, 11, 23, 20, 15);
        LocalDateTime end = LocalDateTime.of(2026, 11, 23, 20, 20);
        when(databaseService.executePreparedStatement(connection, EmailsSentDB.class,
            EmailsSent.EMAILS_SENT_SQL, start, end)).thenReturn(response);

        assertEquals(Job.Result.passed(), emailsSent.populate());

        verify(databaseService).executePreparedStatement(connection, EmailsSentDB.class,
            EmailsSent.EMAILS_SENT_SQL, start, end);
        assertArrayEquals(new String[]{"5221", "SUMMONS ENGLISH", "12"}, emailsSent.rows.get(1));
        assertArrayEquals(new String[]{"5224AC", "CONFIRMATION WELSH", "0"}, emailsSent.rows.get(2));
        verify(emailsSent).populateTimestamp(dashboardData, "Emails Sent", LocalDateTime.now(clock));
    }

    @Test
    void positivePopulateUsesFridayWindowForSaturdayRun() {
        clock = Clock.fixed(Instant.parse("2026-11-28T06:00:00Z"), ZoneOffset.UTC);
        emailsSent = spy(new EmailsSent(dashboardData, databaseService, databaseConfig, clock));
        doNothing().when(emailsSent).populateTimestamp(any(), any(), any(LocalDateTime.class));
        LocalDateTime start = LocalDateTime.of(2026, 11, 27, 20, 15);
        LocalDateTime end = LocalDateTime.of(2026, 11, 27, 20, 20);
        when(databaseService.executePreparedStatement(connection, EmailsSentDB.class,
            EmailsSent.EMAILS_SENT_SQL, start, end)).thenReturn(List.of(
                new EmailsSentDB().setType("5221").setDescription("SUMMONS ENGLISH").setCount(1L)));

        assertEquals(Job.Result.passed(), emailsSent.populate());

        verify(databaseService).executePreparedStatement(connection, EmailsSentDB.class,
            EmailsSent.EMAILS_SENT_SQL, start, end);
    }

    @Test
    void negativePopulateEmptyResponse() {
        mockResponse(List.of());

        assertEquals(Job.Result.failed("No response from database"), emailsSent.populate());

        assertArrayEquals(new String[]{"ERROR", "ERROR", "ERROR"}, emailsSent.rows.get(1));
        verify(emailsSent).populateTimestamp(dashboardData, "Emails Sent", LocalDateTime.now(clock));
    }

    @Test
    void negativePopulateNullResponse() {
        mockResponse(null);

        assertEquals(Job.Result.failed("No response from database"), emailsSent.populate());

        assertArrayEquals(new String[]{"ERROR", "ERROR", "ERROR"}, emailsSent.rows.get(1));
    }

    @Test
    void negativePopulateUnexpectedException() {
        RuntimeException cause = new RuntimeException("Database unavailable");
        LocalDateTime start = LocalDateTime.of(2026, 11, 23, 20, 15);
        LocalDateTime end = LocalDateTime.of(2026, 11, 23, 20, 20);
        when(databaseService.executePreparedStatement(connection, EmailsSentDB.class,
            EmailsSent.EMAILS_SENT_SQL, start, end)).thenThrow(cause);

        assertEquals(Job.Result.failed("Unexpected exception", cause), emailsSent.populate());

        assertArrayEquals(new String[]{"ERROR", "ERROR", "ERROR"}, emailsSent.rows.get(1));
        verify(emailsSent, times(1))
            .populateTimestamp(dashboardData, "Emails Sent", LocalDateTime.now(clock));
    }

    private void mockResponse(List<EmailsSentDB> response) {
        LocalDateTime start = LocalDateTime.of(2026, 11, 23, 20, 15);
        LocalDateTime end = LocalDateTime.of(2026, 11, 23, 20, 20);
        when(databaseService.executePreparedStatement(connection, EmailsSentDB.class,
            EmailsSent.EMAILS_SENT_SQL, start, end)).thenReturn(response);
    }
}
