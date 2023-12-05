package uk.gov.hmcts.juror.job.execution.jobs.stats;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import uk.gov.hmcts.juror.job.execution.config.DatabaseConfig;
import uk.gov.hmcts.juror.job.execution.config.SmtpConfig;
import uk.gov.hmcts.juror.job.execution.database.model.MetaData;
import uk.gov.hmcts.juror.job.execution.jobs.Job;
import uk.gov.hmcts.juror.job.execution.model.Status;
import uk.gov.hmcts.juror.job.execution.service.contracts.DatabaseService;
import uk.gov.hmcts.juror.job.execution.service.contracts.SmtpService;

import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class PerformanceStatsJobTest {

    private PerformanceStatsConfig config;
    private DatabaseService databaseService;
    private SmtpService smtpService;

    private PerformanceStatsJob performanceStatsJob;

    @BeforeEach
    void beforeEach() {
        databaseService = mock(DatabaseService.class);
        smtpService = mock(SmtpService.class);
        config = createConfig();
        performanceStatsJob = spy(new PerformanceStatsJob(databaseService, smtpService, config));
    }

    private PerformanceStatsConfig createConfig() {
        PerformanceStatsConfig performanceStatsConfig = new PerformanceStatsConfig();
        performanceStatsConfig.setDatabase(mock(DatabaseConfig.class));
        performanceStatsConfig.setSmtp(mock(SmtpConfig.class));
        performanceStatsConfig.setResponseTimesAndNonRespondNoMonths(RandomUtils.nextInt());
        performanceStatsConfig.setWelshOnlineResponsesNoMonths(RandomUtils.nextInt());
        performanceStatsConfig.setThirdpartyOnlineNoMonths(RandomUtils.nextInt());
        performanceStatsConfig.setDeferralsNoMonths(RandomUtils.nextInt());
        performanceStatsConfig.setExcusalsNoMonths(RandomUtils.nextInt());
        performanceStatsConfig.setEmailRecipients(new String[]{
            RandomStringUtils.randomAlphabetic(1, 10),
            RandomStringUtils.randomAlphabetic(1, 10),
            RandomStringUtils.randomAlphabetic(1, 10)
        });
        return performanceStatsConfig;
    }

    @Test
    void positiveConstructorTest() {
        assertSame(databaseService, performanceStatsJob.getDatabaseService());
        assertSame(smtpService, performanceStatsJob.getSmtpService());
        assertSame(config, performanceStatsJob.getConfig());
    }

    @Test
    void positiveGetResultSuppliersTest() {
        Job.Result result = mock(Job.Result.class);
        MetaData metaData = mock(MetaData.class);
        doNothing().when(performanceStatsJob).sendEmailWithResult(result);
        doReturn(result).when(performanceStatsJob).runRunProcedure(any());
        doReturn(result).when(performanceStatsJob).runRunProcedure(any(), any());

        List<Job.ResultSupplier> resultSuppliers = performanceStatsJob.getResultSuppliers();

        assertEquals(2, resultSuppliers.size(),
            "There should be 2 result suppliers");

        verify(performanceStatsJob, times(1)).getResultSuppliers();
        verifyNoMoreInteractions(databaseService, smtpService);
        verifyNoMoreInteractions(performanceStatsJob);

        Job.ResultSupplier resultSupplier1 = resultSuppliers.get(0);

        assertFalse(resultSupplier1.isContinueOnFailure(),
            "The first result supplier should not continue on failure");

        assertEquals(1, resultSupplier1.getResultRunners().size(),
            "There should be 1 result runner");

        Function<MetaData, Job.Result> runner1 = resultSupplier1.getResultRunners().iterator().next();
        runner1.apply(metaData);
        verify(performanceStatsJob, times(1))
            .runRunProcedure("refresh_stats_data.auto_processed");
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
            .runRunProcedure("refresh_stats_data.response_times_and_non_respond",
                this.config.getResponseTimesAndNonRespondNoMonths());
        verify(performanceStatsJob, times(1))
            .runRunProcedure("refresh_stats_data.unprocessed_responses");
        verify(performanceStatsJob, times(1))
            .runRunProcedure("refresh_stats_data.welsh_online_responses",
                this.config.getWelshOnlineResponsesNoMonths());
        verify(performanceStatsJob, times(1))
            .runRunProcedure("refresh_stats_data.thirdparty_online",
                this.config.getThirdpartyOnlineNoMonths());
        verify(performanceStatsJob, times(1))
            .runRunProcedure("refresh_stats_data.deferrals",
                this.config.getDeferralsNoMonths());
        verify(performanceStatsJob, times(1))
            .runRunProcedure("refresh_stats_data.excusals",
                this.config.getExcusalsNoMonths());

        verifyNoMoreInteractions(databaseService, smtpService);
        verifyNoMoreInteractions(performanceStatsJob);

        resultSupplier2.runPostActions(result);
        verify(performanceStatsJob, times(1)).sendEmailWithResult(result);
    }

    @Test
    void positiveSendEmailWithResultTest() {
        Job.Result result = Job.Result.passed();
        performanceStatsJob.sendEmailWithResult(result);
        verify(smtpService, times(1))
            .sendEmail(config.getSmtp(), "Performance Dashboard Success",
                "Performance Dashboard procedures have run, with no errors.",
                config.getEmailRecipients());
    }

    @ParameterizedTest
    @EnumSource(value = Status.class,
        mode = EnumSource.Mode.EXCLUDE,
        names = {"SUCCESS"})
    void negativeSendEmailWithResultTestNotSuccess(Status status) {
        Job.Result result = new Job.Result(status, "Some failure reason");
        performanceStatsJob.sendEmailWithResult(result);
        verify(smtpService, times(1))
            .sendEmail(config.getSmtp(), "Performance Dashboard with Error",
                "Performance Dashboard procedures have run, and errors were found.\n\n\n"
                    + "Error message:\n"
                    + result.getMessage(),
                config.getEmailRecipients());
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
