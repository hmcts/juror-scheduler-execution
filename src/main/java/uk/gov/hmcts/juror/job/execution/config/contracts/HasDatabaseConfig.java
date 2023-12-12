package uk.gov.hmcts.juror.job.execution.config.contracts;

import uk.gov.hmcts.juror.job.execution.config.DatabaseConfig;

public interface HasDatabaseConfig {

    DatabaseConfig getDatabase();

    void setDatabase(DatabaseConfig databaseConfig);
}
