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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class ExpensesTest {

    private DashboardData dashboardData;
    private Expenses expenses;
    private DatabaseService databaseService;
    private DatabaseConfig databaseConfig;
    private Clock clock;

    @BeforeEach
    void beforeEach() {
        this.dashboardData = mock(DashboardData.class);
        this.databaseService = mock(DatabaseService.class);
        this.databaseConfig = mock(DatabaseConfig.class);
        this.clock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
        this.expenses = spy(new Expenses(dashboardData, databaseService, databaseConfig, clock));
    }

    @Test
    void positiveConstructorTest() {
        assertSame(dashboardData, expenses.dashboardData,
            "DashboardData should be the same");
        assertSame(databaseService, expenses.databaseService,
            "DatabaseService should be the same");
        assertSame(databaseConfig, expenses.databaseConfig,
            "databaseConfig should be the same");
        assertSame(clock, expenses.clock,
            "Clock should be the same");

        assertEquals("Expenses", expenses.title,
            "Expected title to be Expenses");
        assertEquals(3, expenses.columCount,
            "Expected column count to be 3");
        assertEquals(3, expenses.rows.get(0).length,
            "Expected row size to be 3");
        assertEquals("Type", expenses.rows.get(0)[0],
            "Expected row 0 column 0 to be Type");
        assertEquals("Date", expenses.rows.get(0)[1],
            "Expected row 0 column 1 to be Date");
        assertEquals("Amount", expenses.rows.get(0)[2],
            "Expected row 0 column 2 to be Amount");
    }

    @Test
    void positiveAddRowTest() {
        assertEquals(1, expenses.rows.size());
        expenses.addRow("someDate", "someAmount");
        assertEquals(2, expenses.rows.size());
        assertEquals(3, expenses.rows.get(1).length);
        assertEquals("Payment file total", expenses.rows.get(1)[0],
            "Expected row 1 column 0 to be Payment file total");
        assertEquals("someDate", expenses.rows.get(1)[1],
            "Expected row 1 column 1 to be someDate");
        assertEquals("someAmount", expenses.rows.get(1)[2],
            "Expected row 1 column 2 to be someAmount");

    }

    @Test
    void positivePopulateTest() {
        Connection connection = mock(Connection.class);
        doAnswer(invocation -> {
            Consumer<Connection> connectionConsumer = invocation.getArgument(1);
            connectionConsumer.accept(connection);
            return null;
        }).when(databaseService).execute(eq(databaseConfig), any());

        ExpensesDB expensesDB = new ExpensesDB();
        expensesDB.setAmount("123");
        expensesDB.setDate("someDate");
        List<ExpensesDB> expensesDbList = List.of(expensesDB);
        doReturn(expensesDbList).when(databaseService)
            .executePreparedStatement(connection, ExpensesDB.class, Expenses.EXPENSES_SQL);
        doNothing().when(expenses).populateTimestamp(any(), any(), any(LocalDateTime.class));

        assertEquals(Job.Result.passed(), expenses.populate(),
            "Expected populate to return passed");

        verify(databaseService, times(1)).execute(eq(databaseConfig), any());
        verify(databaseService, times(1))
            .executePreparedStatement(connection, ExpensesDB.class, Expenses.EXPENSES_SQL);
        verify(expenses, times(1)).addRow("someDate", "123");
        verify(expenses, times(1)).populateTimestamp(dashboardData, "Expenses", LocalDateTime.now(clock));

    }

    @Test
    void negativePopulateNullResponseTest() {
        Connection connection = mock(Connection.class);
        doAnswer(invocation -> {
            Consumer<Connection> connectionConsumer = invocation.getArgument(1);
            connectionConsumer.accept(connection);
            return null;
        }).when(databaseService).execute(eq(databaseConfig), any());

        doReturn(null).when(databaseService)
            .executePreparedStatement(connection, ExpensesDB.class, Expenses.EXPENSES_SQL);
        doNothing().when(expenses).populateTimestamp(any(), any(), any(LocalDateTime.class));

        assertEquals(Job.Result.failed("No response from database"), expenses.populate(),
            "Expected populate to return passed");

        verify(databaseService, times(1)).execute(eq(databaseConfig), any());
        verify(databaseService, times(1))
            .executePreparedStatement(connection, ExpensesDB.class, Expenses.EXPENSES_SQL);
        verify(expenses, times(1)).addRow("ERROR", "ERROR");
        verify(expenses, times(1)).populateTimestamp(dashboardData, "Expenses", LocalDateTime.now(clock));
    }

    @Test
    void negativePopulateEmptyResponseTest() {
        Connection connection = mock(Connection.class);
        doAnswer(invocation -> {
            Consumer<Connection> connectionConsumer = invocation.getArgument(1);
            connectionConsumer.accept(connection);
            return null;
        }).when(databaseService).execute(eq(databaseConfig), any());

        doReturn(List.of()).when(databaseService)
            .executePreparedStatement(connection, ExpensesDB.class, Expenses.EXPENSES_SQL);
        doNothing().when(expenses).populateTimestamp(any(), any(), any(LocalDateTime.class));

        assertEquals(Job.Result.failed("No response from database"), expenses.populate(),
            "Expected populate to return passed");

        verify(databaseService, times(1)).execute(eq(databaseConfig), any());
        verify(databaseService, times(1))
            .executePreparedStatement(connection, ExpensesDB.class, Expenses.EXPENSES_SQL);
        verify(expenses, times(1)).addRow("ERROR", "ERROR");
        verify(expenses, times(1)).populateTimestamp(dashboardData, "Expenses", LocalDateTime.now(clock));
    }

    @Test
    void negativePopulateUnexpectedExceptionTest() {
        RuntimeException cause = new RuntimeException("I am the cause");
        Connection connection = mock(Connection.class);
        doAnswer(invocation -> {
            Consumer<Connection> connectionConsumer = invocation.getArgument(1);
            connectionConsumer.accept(connection);
            return null;
        }).when(databaseService).execute(eq(databaseConfig), any());

        doThrow(cause).when(databaseService)
            .executePreparedStatement(connection, ExpensesDB.class, Expenses.EXPENSES_SQL);
        doNothing().when(expenses).populateTimestamp(any(), any(), any(LocalDateTime.class));

        assertEquals(Job.Result.failed("Unexpected exception", cause), expenses.populate(),
            "Expected populate to return passed");

        verify(databaseService, times(1)).execute(eq(databaseConfig), any());
        verify(databaseService, times(1))
            .executePreparedStatement(connection, ExpensesDB.class, Expenses.EXPENSES_SQL);
        verify(expenses, times(1)).addRow("ERROR", "ERROR");
        verify(expenses, times(1)).populateTimestamp(dashboardData, "Expenses", LocalDateTime.now(clock));
    }
}
