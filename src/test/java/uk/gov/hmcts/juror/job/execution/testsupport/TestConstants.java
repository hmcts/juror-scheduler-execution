package uk.gov.hmcts.juror.job.execution.testsupport;

import uk.gov.hmcts.juror.job.execution.database.model.MetaData;

import java.util.Map;

public class TestConstants {

    public static final String VALID_JOB_KEY = "ABC";
    public static final String VALID_TASK_ID = "1";
    public static final Map<String, String> REQUEST_PARAMS = Map.of(
        "jobKey", VALID_JOB_KEY,
        "taskId", VALID_TASK_ID);
    public static final Long VALID_TASK_ID_LONG = Long.parseLong(VALID_TASK_ID);

    public static final MetaData VALID_META_DATA = new MetaData(VALID_JOB_KEY, VALID_TASK_ID_LONG, REQUEST_PARAMS);
    public static final String VALID_JUROR_NUMBER = "123456789";

    private TestConstants() {

    }
}
