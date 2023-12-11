package uk.gov.hmcts.juror.job.execution.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import uk.gov.hmcts.juror.job.execution.client.contracts.SchedulerServiceClient;
import uk.gov.hmcts.juror.job.execution.database.model.MetaData;
import uk.gov.hmcts.juror.job.execution.jobs.Job;
import uk.gov.hmcts.juror.job.execution.model.Status;
import uk.gov.hmcts.juror.job.execution.testsupport.TestConstants;
import uk.gov.hmcts.juror.standard.service.exceptions.NotFoundException;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JobServiceImplTest {
    private static final Instant NOW = Instant.now();
    private static final List<Job> JOB_LIST = List.of(
        new TestJob1(),
        new TestJob2(),
        new TestJob3()
    );
    private Clock clock;

    private SchedulerServiceClient schedulerServiceClient;

    private JobServiceImpl jobService;

    @BeforeEach
    void beforeEach() {
        this.clock = mock(Clock.class);
        when(clock.instant()).thenReturn(NOW, NOW.plusSeconds(10));
        this.schedulerServiceClient = mock(SchedulerServiceClient.class);
        this.jobService = new JobServiceImpl(clock, schedulerServiceClient, JOB_LIST);
    }

    @Test
    void positiveConstructorTest() {
        assertSame(clock, jobService.clock);
        assertSame(schedulerServiceClient, jobService.schedulerServiceClient);
        assertEquals(3, jobService.jobRunners.size());
        assertEquals(JOB_LIST.get(0), jobService.jobRunners.get("TestJob1"));
        assertEquals(JOB_LIST.get(1), jobService.jobRunners.get("TestJob2"));
        assertEquals(JOB_LIST.get(2), jobService.jobRunners.get("TestJob3"));
    }


    @Test
    void positiveGetJob() {
        assertEquals(JOB_LIST.get(0), jobService.getJob("TestJob1"));
        assertEquals(JOB_LIST.get(1), jobService.getJob("TestJob2"));
        assertEquals(JOB_LIST.get(2), jobService.getJob("TestJob3"));
    }

    @Test
    void negativeGetJobNotFound() {
        NotFoundException notFoundException =
            assertThrows(NotFoundException.class, () -> jobService.getJob("TestJob"));
        assertEquals("Job with name: TestJob not found", notFoundException.getMessage());
    }

    @Test
    void positiveTriggerJob() {
        Job job = mock(Job.class);
        MetaData metaData = TestConstants.VALID_META_DATA;

        Job.Result result = mock(Job.Result.class);
        when(job.execute(metaData)).thenReturn(result);
        when(result.getStatus()).thenReturn(Status.SUCCESS);
        when(result.getMessage()).thenReturn("Test message");
        when(result.getMetaData()).thenReturn(new ConcurrentHashMap<>());
        jobService.trigger(job, metaData);


        ArgumentCaptor<SchedulerServiceClient.StatusUpdatePayload> argumentCaptor =
            ArgumentCaptor.forClass(SchedulerServiceClient.StatusUpdatePayload.class);

        verify(job, times(1)).execute(metaData);
        verify(schedulerServiceClient, times(1))
            .updateStatus(eq(TestConstants.VALID_JOB_KEY),
                eq(TestConstants.VALID_TASK_ID_LONG),
                argumentCaptor.capture()
            );

        SchedulerServiceClient.StatusUpdatePayload payload = argumentCaptor.getValue();
        assertEquals(Status.SUCCESS, payload.getStatus());
        assertEquals("Test message", payload.getMessage());
        assertEquals(result.getMetaData(), payload.getMetaData());
    }

    static class TestJob extends Job {
        @Override
        public Result executeRunners(MetaData metaData) {
            return Result.passed();
        }

    }

    static class TestJob1 extends TestJob {
    }

    static class TestJob2 extends TestJob {
    }

    static class TestJob3 extends TestJob {
    }

}
