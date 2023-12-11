package uk.gov.hmcts.juror.job.execution.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.juror.job.execution.client.contracts.SchedulerServiceClient;
import uk.gov.hmcts.juror.job.execution.database.model.MetaData;
import uk.gov.hmcts.juror.job.execution.jobs.Job;
import uk.gov.hmcts.juror.job.execution.service.contracts.JobService;
import uk.gov.hmcts.juror.standard.service.exceptions.NotFoundException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class JobServiceImpl implements JobService {

    final Clock clock;
    final Map<String, Job> jobRunners;
    final SchedulerServiceClient schedulerServiceClient;

    @Autowired
    public JobServiceImpl(Clock clock, SchedulerServiceClient schedulerServiceClient,
                          List<Job> jobList) {
        this.clock = clock;
        this.jobRunners = new ConcurrentHashMap<>();
        this.schedulerServiceClient = schedulerServiceClient;
        jobList.forEach(job -> {
            String jobName = job.getClass().getSimpleName().replaceAll("\\$\\$.*", "");
            this.jobRunners.put(jobName, job);
            log.info("Job loaded: " + jobName);
        });
    }

    @Override
    public Job getJob(String jobName) {
        Job job = jobRunners.get(jobName);
        if (job == null) {
            throw new NotFoundException("Job with name: " + jobName + " not found");
        }
        return job;
    }

    @Async
    @Override
    public void trigger(Job job, MetaData metaData) {
        log.info("Job triggered: " + job.getName() + " " + metaData);
        final Instant startTime = clock.instant();
        final Job.Result result = job.execute(metaData);
        final Duration duration = Duration.between(startTime, clock.instant());

        log.info("Job duration: " + duration);
        schedulerServiceClient.updateStatus(
            metaData.getJobKey(),
            metaData.getTaskId(),
            new SchedulerServiceClient.StatusUpdatePayload(
                result.getStatus(),
                result.getMessage(),
                result.getMetaData()
            )
        );
    }

}
