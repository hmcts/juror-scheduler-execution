package uk.gov.hmcts.juror.job.execution.service.contracts;

import uk.gov.hmcts.juror.job.execution.database.model.MetaData;
import uk.gov.hmcts.juror.job.execution.jobs.Job;

public interface JobService {
    void trigger(Job job, MetaData metaData);

    Job getJob(String jobKey);
}
