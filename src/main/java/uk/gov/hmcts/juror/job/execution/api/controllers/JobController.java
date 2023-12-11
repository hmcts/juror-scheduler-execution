package uk.gov.hmcts.juror.job.execution.api.controllers;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.juror.job.execution.client.contracts.SchedulerServiceClient;
import uk.gov.hmcts.juror.job.execution.config.PermissionConstants;
import uk.gov.hmcts.juror.job.execution.database.model.MetaData;
import uk.gov.hmcts.juror.job.execution.jobs.Job;
import uk.gov.hmcts.juror.job.execution.jobs.checks.pnc.batch.PncBatchJob;
import uk.gov.hmcts.juror.job.execution.service.contracts.JobService;


@RestController()
@RequestMapping(value = "/job", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
public class JobController {
    private final JobService jobService;
    private final PncBatchJob pncBatchJob;

    @Autowired
    public JobController(JobService jobService,
                         PncBatchJob pncBatchJob) {
        this.jobService = jobService;
        this.pncBatchJob = pncBatchJob;
    }

    @PutMapping("/trigger")
    @PreAuthorize("hasAuthority('" + PermissionConstants.TRIGGER + "')")
    public ResponseEntity<Void> triggerJob(
        @RequestParam(name = "job_name")
        @NotNull String jobName,
        @RequestHeader(value = "job_key", required = false) String jobKey,
        @RequestHeader(value = "task_id", required = false) Long taskId
    ) {
        //Two calls to JobService are required to allow async to work
        Job job = this.jobService.getJob(jobName);
        this.jobService.trigger(job, new MetaData(jobKey, taskId));
        return ResponseEntity.accepted().build();
    }

    @PutMapping("/{jobKey}/{taskId}/update/pnc")
    @PreAuthorize("hasAuthority('" + PermissionConstants.UPDATE_PNC + "')")
    public ResponseEntity<Void> updatePncJobStatus(
        @Valid @RequestBody SchedulerServiceClient.StatusUpdatePayload payload,
        @PathVariable("jobKey") String jobKey,
        @PathVariable("taskId") Long taskId
    ) {
        this.pncBatchJob.updateResult(payload, jobKey, taskId);
        return ResponseEntity.accepted().build();
    }
}
