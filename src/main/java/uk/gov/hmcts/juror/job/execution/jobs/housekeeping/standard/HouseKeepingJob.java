package uk.gov.hmcts.juror.job.execution.jobs.housekeeping.standard;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.juror.job.execution.jobs.Job;
import uk.gov.hmcts.juror.job.execution.jobs.StoredProcedureJob;
import uk.gov.hmcts.juror.job.execution.service.contracts.DatabaseService;

@Component
@Getter
@Setter
public class HouseKeepingJob extends StoredProcedureJob {

    static final String HOUSEKEEPING_PROCEDURE = "juror_mod.housekeeping_process";
    static final String DEACTIVATE_INACTIVE_USERS_PROCEDURE = "juror_mod.deactivate_inactive_users";

    private final int inactiveUserThresholdMonths;

    protected HouseKeepingJob(DatabaseService databaseService, HouseKeepingConfig config) {
        super(databaseService, config.getDatabase(), HOUSEKEEPING_PROCEDURE,
            config.getMaxTimeout(), config.getOwnerRestrict());
        this.inactiveUserThresholdMonths = config.getInactiveUserThresholdMonths();
    }

    @Override
    public Job.Result executeStoredProcedure() {
        this.databaseService.execute(getDatabaseConfig(), connection -> {
            this.databaseService.executeStoredProcedure(connection, HOUSEKEEPING_PROCEDURE, procedureArguments);
            this.databaseService.executeStoredProcedure(connection, DEACTIVATE_INACTIVE_USERS_PROCEDURE,
                inactiveUserThresholdMonths);
        });
        return Job.Result.passed();
    }
}
