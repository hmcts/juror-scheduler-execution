package uk.gov.hmcts.juror.job.execution.jobs.housekeeping.standard;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.juror.job.execution.jobs.StoredProcedureJob;
import uk.gov.hmcts.juror.job.execution.service.contracts.DatabaseService;

@Component
@Getter
@Setter
public class HouseKeepingJob extends StoredProcedureJob {

    protected HouseKeepingJob(DatabaseService databaseService, HouseKeepingConfig config) {
        super(databaseService, config.getDatabase(), "juror_mod.housekeeping_process",
            config.getMaxTimeout(), config.getOwnerRestrict());
    }
}
