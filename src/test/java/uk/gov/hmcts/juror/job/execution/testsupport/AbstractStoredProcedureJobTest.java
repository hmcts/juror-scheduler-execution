package uk.gov.hmcts.juror.job.execution.testsupport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import uk.gov.hmcts.juror.job.execution.config.DatabaseConfig;
import uk.gov.hmcts.juror.job.execution.config.contracts.HasDatabaseConfig;
import uk.gov.hmcts.juror.job.execution.database.model.MetaData;
import uk.gov.hmcts.juror.job.execution.jobs.Job;
import uk.gov.hmcts.juror.job.execution.jobs.StoredProcedureJob;
import uk.gov.hmcts.juror.job.execution.model.Status;
import uk.gov.hmcts.juror.job.execution.service.contracts.DatabaseService;

import java.sql.Connection;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public abstract class AbstractStoredProcedureJobTest<J extends StoredProcedureJob,
    C extends HasDatabaseConfig> {

    private DatabaseService databaseService;
    private DatabaseConfig databaseConfig;
    private final String procedureName;


    protected AbstractStoredProcedureJobTest(String procedureName) {
        this.procedureName = procedureName;
    }

    @BeforeEach
    void beforeEach() {
        this.databaseService = mock(DatabaseService.class);
        this.databaseConfig = mock(DatabaseConfig.class);
    }

    public abstract J createStoredProcedureJob(DatabaseService databaseService,
                                               C config);


    public abstract C createConfig();

    @Test
    protected void positiveConstructorTest() {
        C config = createConfig();
        config.setDatabase(databaseConfig);

        J job = createStoredProcedureJob(this.databaseService, config);

        assertSame(this.databaseService, job.getDatabaseService(), "DatabaseService should be the same");
        assertSame(this.databaseConfig, job.getDatabaseConfig(), "DatabaseConfig should be the same");
        assertEquals(this.procedureName, job.getProcedureName(), "ProcedureName should be the same");

        Object[] procedureArguments = this.getProcedureArguments(config);
        assertEquals(procedureArguments.length, job.getProcedureArguments().length,
            "ProcedureArguments should be the same");
        for (int index = 0; index < procedureArguments.length; index++) {
            assertEquals(procedureArguments[index], job.getProcedureArguments()[index],
                "ProcedureArguments should be the same");
        }
    }

    @Test
    void positiveGetResultSupplierTest() {
        C config = createConfig();
        config.setDatabase(databaseConfig);
        J job = createStoredProcedureJob(this.databaseService, config);

        job = spy(job);
        Job.Result expectedResult = Job.Result.passed();
        doReturn(expectedResult).when(job).executeStoredProcedure();

        Job.ResultSupplier resultSupplier = job.getResultSupplier();

        assertNotNull(resultSupplier.getPostRunChecks(), "ResultSupplier should not be null");


        assertFalse(resultSupplier.isContinueOnFailure(), "ContinueOnFailure should be false");

        Collection<Function<MetaData, Job.Result>> resultRunners = resultSupplier.getResultRunners();
        assertEquals(1, resultRunners.size(), "ResultRunners only have on step");
        Optional<Function<MetaData, Job.Result>> runnerOptional = resultRunners.stream().findFirst();
        assertTrue(runnerOptional.isPresent(), "ResultRunners should have a step");

        Function<MetaData, Job.Result> runner = runnerOptional.get();
        Job.Result result = runner.apply(null);
        assertSame(expectedResult, result, "Result should be the same");
        verify(job, times(1)).executeStoredProcedure();

        //Verify postRunChecks do nothing
        resultSupplier.getPostRunChecks().accept(Job.Result.passed());

        verifyNoMoreInteractions(databaseService);
    }

    @Test
    @SuppressWarnings("unchecked")
    void positiveExecuteStoredProcedure() {
        C config = createConfig();
        config.setDatabase(databaseConfig);
        J job = createStoredProcedureJob(this.databaseService, config);



        Job.Result result = job.executeStoredProcedure();


        ArgumentCaptor<Consumer<Connection>> connectionConsumerCaptor = ArgumentCaptor.forClass(Consumer.class);

        verify(databaseService,times(1))
            .execute(eq(databaseConfig), connectionConsumerCaptor.capture());

        Consumer<Connection> connectionConsumer = connectionConsumerCaptor.getValue();
        Connection connection = mock(Connection.class);
        connectionConsumer.accept(connection);

        verify(databaseService,times(1))
            .executeStoredProcedure(connection, procedureName, job.getProcedureArguments());

        assertEquals(Status.SUCCESS, result.getStatus(), "Status should be SUCCESS");
        assertNull(result.getMessage(), "ErrorMessage should be null");
        assertNull(result.getThrowable(), "Exception should be null");
        assertEquals(0, result.getMetaData().size(), "MetaData should be empty");

        verifyNoMoreInteractions(databaseService);
    }


    protected Object[] getProcedureArguments(C config) {
        return new Object[0];
    }
}
