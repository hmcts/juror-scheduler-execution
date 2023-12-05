package uk.gov.hmcts.juror.job.execution.jobs.housekeeping.standard;

import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.hmcts.juror.job.execution.config.DatabaseConfig;
import uk.gov.hmcts.juror.job.execution.jobs.Job;
import uk.gov.hmcts.juror.job.execution.model.Status;
import uk.gov.hmcts.juror.job.execution.service.contracts.DatabaseService;

import java.sql.Connection;
import java.sql.Types;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class HouseKeepingJobTest {

    private HouseKeepingJob houseKeepingJob;
    private DatabaseService databaseService;
    private HouseKeepingConfig config;
    private final String[] resultMessages = new String[]{
        "POOL ERRORS RAISED",
        "FATAL ERROR RAISED",
        "LOG FILE ERROR RAISED",
        "PARAMETER ERROR RAISED",
        "MAX DELETION ERROR RAISED",
        "TIMEOUT ERROR RAISED"
    };

    @BeforeEach
    void beforeEach() {
        databaseService = mock(DatabaseService.class);
        config = createConfig();
        houseKeepingJob = spy(new HouseKeepingJob(databaseService, config));

    }

    private HouseKeepingConfig createConfig() {
        HouseKeepingConfig config = new HouseKeepingConfig();
        config.setDatabase(mock(DatabaseConfig.class));
        config.setReadOnly(false);
        config.setMaxRuntime(RandomUtils.nextInt());
        config.setReadOnly(false);
        return config;
    }

    @Test
    void positiveConstructorTest() {
        assertSame(databaseService, houseKeepingJob.getDatabaseService());
        assertSame(config, houseKeepingJob.getConfig());
    }

    @Test
    void positiveGetResultSuppliersTest() {
        Job.ResultSupplier resultSupplier = houseKeepingJob.getResultSupplier();

        assertEquals(1, resultSupplier.getResultRunners().size(),
            "ResultSupplier should have 1 result runner");
        assertFalse(resultSupplier.isContinueOnFailure(),
            "ResultSupplier should not continue on failure");
        assertNull(resultSupplier.getPostRunChecks(),
            "ResultSupplier should not have pre run action");

        Job.Result result = Job.Result.passed();
        doReturn(result).when(houseKeepingJob).callHouseKeepingProcedure();

        Job.Result resultFromSupplier = resultSupplier.getResultRunners().iterator().next().apply(null);
        assertSame(result, resultFromSupplier,
            "ResultSupplier should return result from callHouseKeepingProcedure");
        verify(houseKeepingJob, times(1)).callHouseKeepingProcedure();

    }

    @Test
    void positiveCallHouseKeepingProcedureTest() {
        Connection connection = mock(Connection.class);

        doAnswer(invocation -> {
            Consumer<Connection> connectionConsumer = invocation.getArgument(1);
            connectionConsumer.accept(connection);
            return null;
        }).when(databaseService).execute(eq(config.getDatabase()), any());

        when(databaseService.executeStoredProcedureWithReturn(
            connection,
            "HOUSEKEEPING.INITIATE_RUN",
            Integer.class,
            Types.INTEGER,
            "NO",
            config.getReadOnly(),
            config.getMaxRuntime()
        )).thenReturn(1);


        Job.Result result = houseKeepingJob.callHouseKeepingProcedure();
        assertEquals(Job.Result.passed(), result,
            "Result should be passed");

        verify(databaseService, times(1))
            .executeStoredProcedureWithReturn(
                connection,
                "HOUSEKEEPING.INITIATE_RUN",
                Integer.class,
                Types.INTEGER,
                "NO",
                config.getReadOnly(),
                config.getMaxRuntime()
            );
        verify(databaseService, times(1))
            .execute(eq(config.getDatabase()), any());
        verifyNoMoreInteractions(databaseService);
    }

    @Test
    void positiveCallHouseKeepingProcedureTestRestrictOwner() {
        Connection connection = mock(Connection.class);

        doAnswer(invocation -> {
            Consumer<Connection> connectionConsumer = invocation.getArgument(1);
            connectionConsumer.accept(connection);
            return null;
        }).when(databaseService).execute(eq(config.getDatabase()), any());
        config.setOwnerRestrict(true);
        when(databaseService.executeStoredProcedureWithReturn(
            connection,
            "HOUSEKEEPING.INITIATE_RUN",
            Integer.class,
            Types.INTEGER,
            "YES",
            config.getReadOnly(),
            config.getMaxRuntime()
        )).thenReturn(1);


        Job.Result result = houseKeepingJob.callHouseKeepingProcedure();
        assertEquals(Job.Result.passed(), result,
            "Result should be passed");

        verify(databaseService, times(1))
            .executeStoredProcedureWithReturn(
                connection,
                "HOUSEKEEPING.INITIATE_RUN",
                Integer.class,
                Types.INTEGER,
                "YES",
                config.getReadOnly(),
                config.getMaxRuntime()
            );
        verify(databaseService, times(1))
            .execute(eq(config.getDatabase()), any());
        verifyNoMoreInteractions(databaseService);

    }

    @ParameterizedTest
    @ValueSource(ints = {2, 3, 4, 5, 6})
    void positiveCallHouseKeepingProcedureTestErrorResultCodes(int resultCode) {
        Connection connection = mock(Connection.class);

        doAnswer(invocation -> {
            Consumer<Connection> connectionConsumer = invocation.getArgument(1);
            connectionConsumer.accept(connection);
            return null;
        }).when(databaseService).execute(eq(config.getDatabase()), any());

        when(databaseService.executeStoredProcedureWithReturn(
            connection,
            "HOUSEKEEPING.INITIATE_RUN",
            Integer.class,
            Types.INTEGER,
            "NO",
            config.getReadOnly(),
            config.getMaxRuntime()
        )).thenReturn(resultCode);


        Job.Result result = houseKeepingJob.callHouseKeepingProcedure();
        assertEquals(Job.Result.failed(resultMessages[resultCode - 1]), result,
            "Result should be failed");

        verify(databaseService, times(1))
            .executeStoredProcedureWithReturn(
                connection,
                "HOUSEKEEPING.INITIATE_RUN",
                Integer.class,
                Types.INTEGER,
                "NO",
                config.getReadOnly(),
                config.getMaxRuntime()
            );
        verify(databaseService, times(1))
            .execute(eq(config.getDatabase()), any());
        verifyNoMoreInteractions(databaseService);

    }

    @Test
    void positiveCallHouseKeepingProcedureTestUnknownResultCode() {
        Connection connection = mock(Connection.class);

        doAnswer(invocation -> {
            Consumer<Connection> connectionConsumer = invocation.getArgument(1);
            connectionConsumer.accept(connection);
            return null;
        }).when(databaseService).execute(eq(config.getDatabase()), any());

        when(databaseService.executeStoredProcedureWithReturn(
            connection,
            "HOUSEKEEPING.INITIATE_RUN",
            Integer.class,
            Types.INTEGER,
            "NO",
            config.getReadOnly(),
            config.getMaxRuntime()
        )).thenReturn(7);


        Job.Result result = houseKeepingJob.callHouseKeepingProcedure();
        assertEquals(new Job.Result(Status.FAILED, "Unknown result code: 7"), result,
            "Result should be failed");

        verify(databaseService, times(1))
            .executeStoredProcedureWithReturn(
                connection,
                "HOUSEKEEPING.INITIATE_RUN",
                Integer.class,
                Types.INTEGER,
                "NO",
                config.getReadOnly(),
                config.getMaxRuntime()
            );
        verify(databaseService, times(1))
            .execute(eq(config.getDatabase()), any());
        verifyNoMoreInteractions(databaseService);

    }

    @Test
    void negativeUnexpectedException() {
        RuntimeException cause = new RuntimeException("I am the cause");

        doThrow(cause).when(databaseService).execute(any(), any());

        Job.Result result = houseKeepingJob.callHouseKeepingProcedure();
        assertEquals(Job.Result.failed(cause.getMessage()), result,
            "Result should be failed");

    }
}
