package uk.gov.hmcts.juror.job.execution.jobs;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.juror.job.execution.config.DatabaseConfig;
import uk.gov.hmcts.juror.job.execution.service.contracts.DatabaseService;

import java.util.Set;

@Getter
@Slf4j
public abstract class StoredProcedureJob extends LinearJob {

    protected final DatabaseService databaseService;
    protected final DatabaseConfig databaseConfig;
    protected final String procedureName;
    protected final Object[] procedureArguments;

    protected StoredProcedureJob(DatabaseService databaseService, DatabaseConfig databaseConfig, String procedureName,
                                 Object... procedureArguments) {
        super();
        this.databaseService = databaseService;
        this.databaseConfig = databaseConfig;
        this.procedureName = procedureName;
        this.procedureArguments = procedureArguments.clone();
    }

    @Override
    public ResultSupplier getResultSupplier() {
        return new ResultSupplier(false,
            Set.of(
                metaDate -> executeStoredProcedure()
            ),
            this::postRunChecks);
    }


    public Result executeStoredProcedure() {
        this.databaseService.execute(getDatabaseConfig(),
            connection -> this.databaseService.executeStoredProcedure(connection, this.getProcedureName(),
                procedureArguments));
        return Result.passed();
    }

    @SuppressWarnings("PMD.EmptyMethodInAbstractClassShouldBeAbstract")
    protected void postRunChecks(Job.Result result) {
        // Do nothing by default but here to allow optional implementation
    }
}
