package uk.gov.hmcts.juror.job.execution.jobs.stats;

import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.juror.job.execution.config.DatabaseConfig;
import uk.gov.hmcts.juror.job.execution.database.model.MetaData;
import uk.gov.hmcts.juror.job.execution.jobs.Job;
import uk.gov.hmcts.juror.job.execution.service.contracts.DatabaseService;

import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class PerformanceStatsJobTest {

    private PerformanceStatsConfig config;
    private DatabaseService databaseService;

    private PerformanceStatsJob performanceStatsJob;

    @BeforeEach
    void beforeEach() {
        databaseService = mock(DatabaseService.class);
        config = createConfig();
        performanceStatsJob = spy(new PerformanceStatsJob(databaseService, config));
    }

    private PerformanceStatsConfig createConfig() {
        PerformanceStatsConfig performanceStatsConfig = new PerformanceStatsConfig();
        performanceStatsConfig.setDatabase(mock(DatabaseConfig.class));
        performanceStatsConfig.setResponseTimesAndNonRespondNoMonths(RandomUtils.nextInt());
        performanceStatsConfig.setWelshOnlineResponsesNoMonths(RandomUtils.nextInt());
        performanceStatsConfig.setThirdpartyOnlineNoMonths(RandomUtils.nextInt());
        performanceStatsConfig.setDeferralsNoMonths(RandomUtils.nextInt());
        performanceStatsConfig.setExcusalsNoMonths(RandomUtils.nextInt());
        return performanceStatsConfig;
    }

    @Test
    void positiveConstructorTest() {
        assertSame(databaseService, performanceStatsJob.getDatabaseService());
        assertSame(config, performanceStatsJob.getConfig());
    }

    @Test
    void positiveGetResultSuppliersTest() {
        Job.Result result = mock(Job.Result.class);
        doReturn(result).when(performanceStatsJob).runRunProcedure(any());
        doReturn(result).when(performanceStatsJob).runRunProcedure(any(), any());

        List<Job.ResultSupplier> resultSuppliers = performanceStatsJob.getResultSuppliers();

        assertEquals(2, resultSuppliers.size(),
            "There should be 2 result suppliers");

        verify(performanceStatsJob, times(1)).getResultSuppliers();
        verifyNoMoreInteractions(performanceStatsJob);

        Job.ResultSupplier resultSupplier1 = resultSuppliers.get(0);

        assertFalse(resultSupplier1.isContinueOnFailure(),
            "The first result supplier should not continue on failure");

        assertEquals(1, resultSupplier1.getResultRunners().size(),
            "There should be 1 result runner");

        MetaData metaData = mock(MetaData.class);
        Function<MetaData, Job.Result> runner1 = resultSupplier1.getResultRunners().iterator().next();
        runner1.apply(metaData);
        verify(performanceStatsJob, times(1))
            .runRunProcedure("auto_processed");
        verifyNoMoreInteractions(databaseService, smtpService);
        verifyNoMoreInteractions(performanceStatsJob);


        Job.ResultSupplier resultSupplier2 = resultSuppliers.get(1);
        assertFalse(resultSupplier2.isContinueOnFailure(),
            "The second result supplier should not continue on failure");
        assertEquals(6, resultSupplier2.getResultRunners().size(),
            "There should be 6 result runner");

        for (Function<MetaData, Job.Result> runner : resultSupplier2.getResultRunners()) {
            runner.apply(metaData);
        }
        verify(performanceStatsJob, times(1))
            .runRunProcedure("response_times_and_not_responded",
                this.config.getResponseTimesAndNonRespondNoMonths());
        verify(performanceStatsJob, times(1))
            .runRunProcedure("unprocessed_responses");
        verify(performanceStatsJob, times(1))
            .runRunProcedure("welsh_online_responses",
                this.config.getWelshOnlineResponsesNoMonths());
        verify(performanceStatsJob, times(1))
            .runRunProcedure("thirdparty_online",
                this.config.getThirdpartyOnlineNoMonths());
        verify(performanceStatsJob, times(1))
            .runRunProcedure("deferrals",
                this.config.getDeferralsNoMonths());
        verify(performanceStatsJob, times(1))
            .runRunProcedure("excusals",
                this.config.getExcusalsNoMonths());

        verifyNoMoreInteractions(databaseService);
        verifyNoMoreInteractions(performanceStatsJob);

        resultSupplier2.runPostActions(result);
    }

    @Test
    void positiveRunRunProcedure() {
        Job.Result expectedResult = Job.Result.passed("PASSED: myProcedureName");

        assertEquals(expectedResult, performanceStatsJob.runRunProcedure("myProcedureName"),
            "The result of the procedure should be the expected one");

        verify(databaseService, times(1))
            .executeStoredProcedure(config.getDatabase(), "myProcedureName");

    }

    @Test
    void positiveRunRunProcedureWithArguments() {
        Job.Result expectedResult = Job.Result.passed("PASSED: myProcedureName");

        assertEquals(expectedResult, performanceStatsJob.runRunProcedure("myProcedureName", "ABC", 1, true),
            "The result of the procedure should be the expected one");

        verify(databaseService, times(1))
            .executeStoredProcedure(config.getDatabase(), "myProcedureName", "ABC", 1, true);

    }
}
