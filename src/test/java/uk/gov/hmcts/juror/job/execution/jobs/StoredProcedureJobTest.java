package uk.gov.hmcts.juror.job.execution.jobs;

import org.junit.jupiter.api.Nested;
import uk.gov.hmcts.juror.job.execution.config.DatabaseConfig;
import uk.gov.hmcts.juror.job.execution.config.contracts.HasDatabaseConfig;
import uk.gov.hmcts.juror.job.execution.service.contracts.DatabaseService;
import uk.gov.hmcts.juror.job.execution.testsupport.AbstractStoredProcedureJobTest;

class StoredProcedureJobTest {

    @Nested
    class StoredProcedureJobNoArgumentsTest extends AbstractStoredProcedureJobTest<TestStoredProcedureJobNoArguments,
        TestStoredProcedureJobConfig> {


        protected StoredProcedureJobNoArgumentsTest() {
            super(TestStoredProcedureJobNoArguments.PROCEDURE_NAME);
        }

        @Override
        public TestStoredProcedureJobNoArguments createStoredProcedureJob(DatabaseService databaseService,
                                                                          TestStoredProcedureJobConfig config) {
            return new TestStoredProcedureJobNoArguments(databaseService, config.getDatabase());
        }

        @Override
        public TestStoredProcedureJobConfig createConfig() {
            return new TestStoredProcedureJobConfig();
        }
    }
    @Nested
    class StoredProcedureJobWithArgumentsTest extends AbstractStoredProcedureJobTest<TestStoredProcedureJobWithArguments,
        TestStoredProcedureJobConfig> {


        protected StoredProcedureJobWithArgumentsTest() {
            super(TestStoredProcedureJobWithArguments.PROCEDURE_NAME);
        }

        @Override
        public TestStoredProcedureJobWithArguments createStoredProcedureJob(DatabaseService databaseService,
                                                                          TestStoredProcedureJobConfig config) {
            return new TestStoredProcedureJobWithArguments(databaseService, config.getDatabase());
        }

        @Override
        public TestStoredProcedureJobConfig createConfig() {
            return new TestStoredProcedureJobConfig();
        }

        @Override
        protected Object[] getProcedureArguments(TestStoredProcedureJobConfig config) {
            return TestStoredProcedureJobWithArguments.PROCEDURE_ARGUMENTS;
        }
    }

    public static class TestStoredProcedureJobNoArguments extends StoredProcedureJob {
        private static final String PROCEDURE_NAME = "testProcedureNameWithoutArguments";

        protected TestStoredProcedureJobNoArguments(DatabaseService databaseService,
                                                    DatabaseConfig databaseConfig) {
            super(databaseService, databaseConfig, PROCEDURE_NAME);
        }
    }

    public static class TestStoredProcedureJobWithArguments extends StoredProcedureJob {
        static final String PROCEDURE_NAME = "testProcedureNameWithArguments";
        static final Object[] PROCEDURE_ARGUMENTS = new Object[]{"argumentString", 1, true};

        protected TestStoredProcedureJobWithArguments(DatabaseService databaseService,
                                                      DatabaseConfig databaseConfig) {
            super(databaseService, databaseConfig, PROCEDURE_NAME, PROCEDURE_ARGUMENTS);
        }
    }
    public static class TestStoredProcedureJobConfig implements HasDatabaseConfig {

        private DatabaseConfig databaseConfig;

        @Override
        public DatabaseConfig getDatabase() {
            return this.databaseConfig;
        }

        @Override
        public void setDatabase(DatabaseConfig databaseConfig) {
            this.databaseConfig = databaseConfig;
        }
    }
}
