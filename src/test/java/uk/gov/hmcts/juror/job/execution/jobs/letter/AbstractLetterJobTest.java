package uk.gov.hmcts.juror.job.execution.jobs.letter;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.juror.job.execution.config.contracts.HasDatabaseConfig;
import uk.gov.hmcts.juror.job.execution.database.model.Count;
import uk.gov.hmcts.juror.job.execution.jobs.Job;
import uk.gov.hmcts.juror.job.execution.service.contracts.DatabaseService;

import java.sql.Connection;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public abstract class AbstractLetterJobTest<J extends LetterJob, C extends HasDatabaseConfig> {

    protected abstract J createJob(DatabaseService databaseService, C config);

    protected abstract C createConfig();

    protected abstract String getCountSql();

    protected abstract String getProcedureName();

    @Test
    protected void positiveConstructorTest() {
        DatabaseService databaseService = mock(DatabaseService.class);
        C config = createConfig();
        config.setDatabase(config.getDatabase());
        J job = createJob(databaseService, config);
        assertSame(databaseService, job.getDatabaseService(),
            "Database service should be the same as the one passed to the constructor");
        assertSame(config.getDatabase(), job.getDatabaseConfig(),
            "Database config should be the same as the one passed to the constructor");
        assertEquals(getProcedureName(), job.getProcedureName(),
            "Procedure name should be the same as the one passed to the constructor");
        assertEquals(getCountSql(), job.getCountSql(),
            "Count SQL should be the same as the one passed to the constructor");
    }


    @Test
    protected void positivePostRunChecksHasCount() {
        DatabaseService databaseService = mock(DatabaseService.class);
        C config = createConfig();
        config.setDatabase(config.getDatabase());
        J job = createJob(databaseService, config);

        Connection connection = mock(Connection.class);

        doAnswer(invocation -> {
            Consumer<Connection> connectionConsumer = invocation.getArgument(1);
            connectionConsumer.accept(connection);
            return null;
        }).when(databaseService).execute(eq(config.getDatabase()), any());

        when(databaseService.executePreparedStatement(connection, Count.class, getCountSql()))
            .thenReturn(List.of(new Count().setValue(3)));

        Job.Result result = Job.Result.passed();
        job.postRunChecks(result);

        assertEquals("3", result.getMetaData().get("LETTERS_AUTOMATICALLY_GENERATED"),
            "LETTERS_AUTOMATICALLY_GENERATED should bee '3' if returned count is 3");

        verify(databaseService, times(1))
            .execute(eq(config.getDatabase()), any());
        verify(databaseService, times(1))
            .executePreparedStatement(connection, Count.class, getCountSql());
        verifyNoMoreInteractions(databaseService);
    }

    @Test
    protected void negativePostRunChecksNoCount() {
        DatabaseService databaseService = mock(DatabaseService.class);
        C config = createConfig();
        config.setDatabase(config.getDatabase());
        J job = createJob(databaseService, config);

        Connection connection = mock(Connection.class);

        doAnswer(invocation -> {
            Consumer<Connection> connectionConsumer = invocation.getArgument(1);
            connectionConsumer.accept(connection);
            return null;
        }).when(databaseService).execute(eq(config.getDatabase()), any());

        when(databaseService.executePreparedStatement(connection, Count.class, getCountSql()))
            .thenReturn(List.of());

        Job.Result result = Job.Result.passed();
        job.postRunChecks(result);

        assertEquals("FAILED", result.getMetaData().get("LETTERS_AUTOMATICALLY_GENERATED"),
            "LETTERS_AUTOMATICALLY_GENERATED should bee 'FAILED' if no count is returned");

        verify(databaseService, times(1))
            .execute(eq(config.getDatabase()), any());
        verify(databaseService, times(1))
            .executePreparedStatement(connection, Count.class, getCountSql());
        verifyNoMoreInteractions(databaseService);

    }
}
