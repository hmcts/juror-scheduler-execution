package uk.gov.hmcts.juror.job.execution.api.controllers;


import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.juror.job.execution.database.model.MetaData;
import uk.gov.hmcts.juror.job.execution.jobs.Job;
import uk.gov.hmcts.juror.job.execution.jobs.checks.pnc.batch.PncBatchJob;
import uk.gov.hmcts.juror.job.execution.model.Status;
import uk.gov.hmcts.juror.job.execution.service.contracts.JobService;
import uk.gov.hmcts.juror.job.execution.testsupport.TestConstants;
import uk.gov.hmcts.juror.job.execution.testsupport.TestUtil;
import uk.gov.hmcts.juror.job.execution.testsupport.controller.ControllerTest;
import uk.gov.hmcts.juror.job.execution.testsupport.controller.ControllerTestWithPayload;
import uk.gov.hmcts.juror.job.execution.testsupport.controller.InvalidPayloadArgument;
import uk.gov.hmcts.juror.job.execution.testsupport.controller.NotFoundArgument;
import uk.gov.hmcts.juror.job.execution.testsupport.controller.RequestArgument;
import uk.gov.hmcts.juror.job.execution.testsupport.controller.SuccessRequestArgument;
import uk.gov.hmcts.juror.standard.api.ExceptionHandling;
import uk.gov.hmcts.juror.standard.service.exceptions.NotFoundException;

import java.util.Map;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = JobController.class,
    excludeAutoConfiguration = {SecurityAutoConfiguration.class})
@ContextConfiguration(
    classes = {
        JobController.class,
        ExceptionHandling.class
    }
)
@DisplayName("Controller: /job")
public class JobControllerTest {
    private static final String CONTROLLER_BASEURL = "/job";
    private static final String TEST_DATA_DIRECTORY = "jobController/";
    @MockBean
    private JobService jobService;
    @MockBean
    private PncBatchJob pncBatchJob;

    @Nested
    @DisplayName("PUT " + Trigger.POST_TRIGGER_URL)
    class Trigger extends ControllerTest {
        static final String POST_TRIGGER_URL = CONTROLLER_BASEURL + "/trigger";
        static final String JOB_NAME_KEY = "job_name";
        static final String JOB_KEY_HEADER_KEY = "job_key";
        static final String TASK_ID_HEADER_KEY = "task_id";

        public Trigger() {
            super(HttpMethod.PUT, POST_TRIGGER_URL, HttpStatus.ACCEPTED);
        }


        protected SuccessRequestArgument createSuccessArgument(String name, Map<String, String> queryParams,
                                                               Map<String, String> headers) {
            Job job = mock(Job.class);
            String jobKey = queryParams.get(JOB_NAME_KEY);
            String taskIdStr = headers.get(TASK_ID_HEADER_KEY);
            Long taskId = (taskIdStr == null ? null : Long.valueOf(taskIdStr));

            SuccessRequestArgument successRequestArgument =
                new SuccessRequestArgument(name,
                    getSuccessStatus(),
                    builder -> {
                        when(jobService.getJob(jobKey)).thenReturn(job);
                    },
                    resultActions -> {
                        verify(jobService, times(1)).getJob(jobKey);
                        verify(jobService, times(1)).trigger(job,
                            new MetaData(headers.get(JOB_KEY_HEADER_KEY), taskId));

                    }, null);
            if (!headers.isEmpty()) {
                successRequestArgument.getHeaders().putAll(headers);
            }
            if (!queryParams.isEmpty()) {
                successRequestArgument.getQueryParams().putAll(queryParams);
            }
            return successRequestArgument;
        }

        @Override
        protected Stream<SuccessRequestArgument> getSuccessRequestArgument() {
            return Stream.of(
                createSuccessArgument("Typical", Map.of(JOB_NAME_KEY, "JobName"),
                    Map.of(JOB_KEY_HEADER_KEY, "JobKeyHeader",
                        TASK_ID_HEADER_KEY, "1")),
                createSuccessArgument("Without Task Id", Map.of(JOB_NAME_KEY, "JobName"),
                    Map.of(JOB_KEY_HEADER_KEY, "JobKeyHeader")),
                createSuccessArgument("Without Job Key", Map.of(JOB_NAME_KEY, "JobName"),
                    Map.of(TASK_ID_HEADER_KEY, "1")),
                createSuccessArgument("Only Job Name", Map.of(JOB_NAME_KEY, "JobName"),
                    Map.of())
            );
        }

        @Test
        @DisplayName("INVALID_PAYLOAD: Job name is missing")
        void negativeJobNameMissing() {
            callAndValidate(new InvalidPayloadArgument(null, "Missing Parameter: job_name"));
        }

        @Test
        @DisplayName("NOT_FOUND: Job name does not exist")
        void negativeJobNotFound() {
            String jobName = "JobName";
            NotFoundArgument notFoundArgument = new NotFoundArgument();
            notFoundArgument.getQueryParams().put(JOB_NAME_KEY, jobName);

            notFoundArgument.setPreActions(builder -> {
                when(jobService.getJob(jobName)).thenThrow(
                    new NotFoundException("Job with name: " + jobName + " not found"));
            });
            notFoundArgument.setPostActions(resultActions -> {
                verify(jobService, times(1)).getJob(jobName);
                verify(jobService, never()).trigger(any(), any());
            });
            callAndValidate(notFoundArgument);
        }

    }

    @Nested
    @DisplayName("PUT " + UpdatePncJobStatus.PUT_PNC_UPDATE)
    class UpdatePncJobStatus extends ControllerTestWithPayload {
        static final String PUT_PNC_UPDATE = CONTROLLER_BASEURL + "/{jobKey}/{taskId}/update/pnc";
        private static final String TEST_DATA_DIRECTORY = JobControllerTest.TEST_DATA_DIRECTORY + "updatePncJobStatus/";


        public UpdatePncJobStatus() {
            super(HttpMethod.PUT, PUT_PNC_UPDATE, HttpStatus.ACCEPTED);
        }

        @Override
        protected Stream<SuccessRequestArgument> getSuccessRequestArgument() {
            final String typicalPayload = getTypicalPayload();
            Stream.Builder<SuccessRequestArgument> builder = Stream.builder();

            builder.add(createSuccessArgument("Typical", typicalPayload));
            for (Status status : Status.values()) {
                builder.add(
                    createSuccessArgument("Only Status", TestUtil.addJsonPath("{}", "$", "status", status.name())));
            }
            builder.add(createSuccessArgument("Status and message", TestUtil.deleteJsonPath(typicalPayload,
                "$.meta_data")));
            builder.add(createSuccessArgument("Status and meta_data", TestUtil.deleteJsonPath(typicalPayload,
                "$.message")));

            builder.add(
                createSuccessArgument("Message Min", TestUtil.replaceJsonPath(typicalPayload, "$.message", "A")));
            builder.add(createSuccessArgument("Message Max", TestUtil.replaceJsonPath(typicalPayload, "$.message",
                RandomStringUtils.randomAlphanumeric(2500))));

            return builder.build();
        }

        private SuccessRequestArgument createSuccessArgument(
            String name,
            String payload
        ) {
            SuccessRequestArgument successRequestArgument = new SuccessRequestArgument(name, getSuccessStatus(),
                null, resultActions ->
                verify(pncBatchJob, times(1)).updateResult(
                    any(),
                    eq(TestConstants.VALID_JOB_KEY),
                    eq(TestConstants.VALID_TASK_ID_LONG)),
                payload);
            successRequestArgument.setPathParams(
                new String[]{TestConstants.VALID_JOB_KEY, TestConstants.VALID_TASK_ID});
            return successRequestArgument;
        }

        @Override
        protected String getTypicalPayload() {
            return TestUtil.getTestDataAsStringFromFile(TEST_DATA_DIRECTORY + "typical.json");
        }

        @Override
        protected Stream<InvalidPayloadArgument> getInvalidPayloadArgumentSource() {
            final String typicalPayload = getTypicalPayload();
            Stream.Builder<InvalidPayloadArgument> builder = Stream.builder();


            builder.add(createInvalidPayloadArgument("Missing status",
                TestUtil.deleteJsonPath(typicalPayload, "$.status"), "status: must not be null"));
            builder.add(createInvalidPayloadArgument("Message too small",
                TestUtil.replaceJsonPath(typicalPayload, "$.message", ""),
                "message: length must be between 1 and 2500"));
            builder.add(createInvalidPayloadArgument("Message too large",
                TestUtil.replaceJsonPath(typicalPayload, "$.message", RandomStringUtils.randomAlphabetic(2501)),
                "message: length must be between 1 and 2500"));

            return builder.build();
        }

        private InvalidPayloadArgument createInvalidPayloadArgument(String name,
                                                                    String payload,
                                                                    String... errorMessages) {
            InvalidPayloadArgument invalidPayloadArgument = new InvalidPayloadArgument(payload, errorMessages);
            invalidPayloadArgument.setPathParams(
                new String[]{TestConstants.VALID_JOB_KEY, TestConstants.VALID_TASK_ID});
            invalidPayloadArgument.setPostActions(
                resultActions -> verify(pncBatchJob, never()).updateResult(any(), any(), any()));
            return invalidPayloadArgument;
        }

        @Override
        protected RequestArgument defaultRequestArgument() {
            RequestArgument requestArgument = new RequestArgument(null, null, null);
            requestArgument.setPathParams(new String[]{TestConstants.VALID_JOB_KEY, TestConstants.VALID_TASK_ID});
            return requestArgument;
        }
    }
}
