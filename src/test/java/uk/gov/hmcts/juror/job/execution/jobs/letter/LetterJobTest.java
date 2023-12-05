package uk.gov.hmcts.juror.job.execution.jobs.letter;

import uk.gov.hmcts.juror.job.execution.config.DatabaseConfig;
import uk.gov.hmcts.juror.job.execution.config.contracts.HasDatabaseConfig;
import uk.gov.hmcts.juror.job.execution.service.contracts.DatabaseService;

class LetterJobTest extends AbstractLetterJobTest<LetterJobTest.TestLetterJob,
    LetterJobTest.TestLetterJobConfig> {


    @Override
    protected TestLetterJob createJob(DatabaseService databaseService, TestLetterJobConfig config) {
        return new TestLetterJob(databaseService, config);
    }

    @Override
    protected TestLetterJobConfig createConfig() {
        return new TestLetterJobConfig();
    }

    @Override
    protected String getCountSql() {
        return TestLetterJob.COUNT_SQL;
    }

    @Override
    protected String getProcedureName() {
        return TestLetterJob.PROCEDURE_NAME;
    }

    public static class TestLetterJobConfig implements HasDatabaseConfig {

        private DatabaseConfig databaseConfig;

        @Override
        public DatabaseConfig getDatabase() {
            return this.databaseConfig;
        }

        @Override
        public void setDatabase(DatabaseConfig databaseConfig) {

        }
    }

    public static class TestLetterJob extends LetterJob {

        public static final String PROCEDURE_NAME = "someTestProcedureName";
        public static final String COUNT_SQL = "SELECT count(1) as count from users";

        public TestLetterJob(DatabaseService databaseService, TestLetterJobConfig config) {
            super(databaseService, config.getDatabase(), PROCEDURE_NAME, COUNT_SQL);
        }
    }
}
