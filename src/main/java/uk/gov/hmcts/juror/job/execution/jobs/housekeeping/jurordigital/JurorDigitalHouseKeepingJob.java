package uk.gov.hmcts.juror.job.execution.jobs.housekeeping.jurordigital;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.juror.job.execution.jobs.StoredProcedureJob;
import uk.gov.hmcts.juror.job.execution.service.contracts.DatabaseService;

@Component
public class JurorDigitalHouseKeepingJob extends StoredProcedureJob {
    protected JurorDigitalHouseKeepingJob(DatabaseService databaseService, JurorDigitalHouseKeepingConfig config) {
        super(databaseService, config.getDatabase(), "juror_digital_housekeeping.perform_deletions",
            config.getRetentionThreshold());
    }
}
