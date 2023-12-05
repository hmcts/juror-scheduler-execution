package uk.gov.hmcts.juror.job.execution.jobs.letter;

import lombok.Getter;
import uk.gov.hmcts.juror.job.execution.client.contracts.SchedulerServiceClient;
import uk.gov.hmcts.juror.job.execution.config.DatabaseConfig;
import uk.gov.hmcts.juror.job.execution.database.model.Count;
import uk.gov.hmcts.juror.job.execution.jobs.Job;
import uk.gov.hmcts.juror.job.execution.jobs.StoredProcedureJob;
import uk.gov.hmcts.juror.job.execution.service.contracts.DatabaseService;

import java.util.List;
import java.util.Map;

@Getter
public abstract class LetterJob extends StoredProcedureJob {

    private final String countSql;
    public static final String TOTAL_LETTERS_GENERATED_META_DATA_KEY = "LETTERS_AUTOMATICALLY_GENERATED";


    protected LetterJob(DatabaseService databaseService, DatabaseConfig config, String procedureName, String countSQL) {
        super(databaseService, config, procedureName);
        this.countSql = countSQL;
    }

    @Override
    protected void postRunChecks(Job.Result result) {
        databaseService.execute(databaseConfig, connection -> {
            List<Count> response =
                databaseService.executePreparedStatement(connection, Count.class, countSql);
            if (response.isEmpty()) {
                result.addMetaData(TOTAL_LETTERS_GENERATED_META_DATA_KEY, "FAILED");
            } else {
                Count count = response.get(0);
                result.addMetaData(TOTAL_LETTERS_GENERATED_META_DATA_KEY, String.valueOf(count.getValue()));
            }
        });
    }
}
