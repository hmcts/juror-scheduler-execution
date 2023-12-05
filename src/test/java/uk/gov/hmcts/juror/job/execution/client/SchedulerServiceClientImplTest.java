package uk.gov.hmcts.juror.job.execution.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.juror.job.execution.client.contracts.SchedulerServiceClient;
import uk.gov.hmcts.juror.job.execution.model.Status;
import uk.gov.hmcts.juror.job.execution.testsupport.TestConstants;
import uk.gov.hmcts.juror.standard.service.exceptions.InternalServerException;
import uk.gov.hmcts.juror.standard.service.exceptions.RemoteGatewayException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
@DisplayName("Client: Scheduler Service")
class SchedulerServiceClientImplTest {

    private SchedulerServiceClientImpl schedulerServiceClient;
    private static final String UPDATE_STATUS_URL = "/job/{job-key}/task/{task-id}/status";
    private static final String GET_LATEST_STATUS_URL = "/job/{job-key}/status";
    private static final String GET_STATUS_URL = "/job/{job-key}/task/{task-id}";

    private RestTemplate restTemplate;

    private ResponseEntity<Void> updateStatusResponse;
    private ResponseEntity<SchedulerServiceClient.TaskResponse> getLatestStatusResponse;
    private ResponseEntity<SchedulerServiceClient.TaskResponse> getStatusResponse;

    @BeforeEach
    void beforeEach() {
        RestTemplateBuilder restTemplateBuilder = mock(RestTemplateBuilder.class);
        restTemplate = mock(RestTemplate.class);
        when(restTemplateBuilder.build()).thenReturn(restTemplate);
        schedulerServiceClient = new SchedulerServiceClientImpl(restTemplateBuilder,
            UPDATE_STATUS_URL, GET_LATEST_STATUS_URL, GET_STATUS_URL);
    }

    @DisplayName("UpdateStatus: " + UPDATE_STATUS_URL)
    @Nested
    class UpdateStatus {
        @BeforeEach
        void beforeEach() {
            updateStatusResponse = mock(ResponseEntity.class);
            when(restTemplate.exchange(
                eq(UPDATE_STATUS_URL),
                eq(HttpMethod.PUT),
                any(),
                eq(Void.class),
                eq(TestConstants.VALID_JOB_KEY),
                eq(TestConstants.VALID_TASK_ID_LONG)))
                .thenReturn(updateStatusResponse);
        }

        private SchedulerServiceClient.StatusUpdatePayload getValidPayload() {
            return new SchedulerServiceClient.StatusUpdatePayload(Status.SUCCESS, null, null);
        }

        @Test
        void negativeNoJobKey() {
            schedulerServiceClient.updateStatus(
                null, TestConstants.VALID_TASK_ID_LONG, getValidPayload());
            verifyNoInteractions(restTemplate);
        }

        @Test
        void negativeBlankJobKey() {
            schedulerServiceClient.updateStatus(
                "", TestConstants.VALID_TASK_ID_LONG, getValidPayload());
            verifyNoInteractions(restTemplate);
        }

        @Test
        void negativeNoTaskId() {
            schedulerServiceClient.updateStatus(
                TestConstants.VALID_JOB_KEY, null, getValidPayload());
            verifyNoInteractions(restTemplate);
        }

        @Test
        void positiveTypical() {
            SchedulerServiceClient.StatusUpdatePayload payload = getValidPayload();

            when(updateStatusResponse.getStatusCode()).thenReturn(HttpStatus.ACCEPTED);

            schedulerServiceClient.updateStatus(
                TestConstants.VALID_JOB_KEY, TestConstants.VALID_TASK_ID_LONG, payload);

            ArgumentCaptor<HttpEntity<SchedulerServiceClient.StatusUpdatePayload>> argumentCaptor =
                ArgumentCaptor.forClass(
                    HttpEntity.class);


            verify(restTemplate, times(1)).exchange(eq(UPDATE_STATUS_URL),
                eq(HttpMethod.PUT),
                argumentCaptor.capture(),
                eq(Void.class),
                eq(TestConstants.VALID_JOB_KEY),
                eq(TestConstants.VALID_TASK_ID_LONG));

            assertEquals(payload, argumentCaptor.getValue().getBody(), "Payloads should match");
        }

        @Test
        void negativeUnexpectedException() {
            SchedulerServiceClient.StatusUpdatePayload payload = getValidPayload();

            RuntimeException cause = new RuntimeException("I am the cause");

            when(restTemplate.exchange(eq(UPDATE_STATUS_URL),
                eq(HttpMethod.PUT),
                any(),
                eq(Void.class),
                eq(TestConstants.VALID_JOB_KEY),
                eq(TestConstants.VALID_TASK_ID_LONG)))
                .thenThrow(cause);

            InternalServerException internalServerException
                = assertThrows(InternalServerException.class,
                () -> schedulerServiceClient.updateStatus(
                    TestConstants.VALID_JOB_KEY, TestConstants.VALID_TASK_ID_LONG, payload),
                "Should throw InternalServerException");

            assertEquals("Failed to upload Job result",
                internalServerException.getMessage(), "Exception message should match");
            assertEquals(cause, internalServerException.getCause(), "Cause should match");
        }

        @Test
        void negativeWrongStatusCode() {
            SchedulerServiceClient.StatusUpdatePayload payload = getValidPayload();

            when(updateStatusResponse.getStatusCode()).thenReturn(HttpStatus.BAD_REQUEST);


            RemoteGatewayException remoteGatewayException
                = assertThrows(RemoteGatewayException.class,
                () -> schedulerServiceClient.updateStatus(
                    TestConstants.VALID_JOB_KEY, TestConstants.VALID_TASK_ID_LONG, payload),
                "Should throw RemoteGatewayException");

            assertEquals("Call to SchedulerServiceClient.updateStatus(jobKey, taskId, payload) "
                    + "failed status code was: 400 BAD_REQUEST",
                remoteGatewayException.getMessage(),
                "Exception message should match");
        }
    }

    @DisplayName("GetLatestTask: " + GET_LATEST_STATUS_URL)
    @Nested
    class GetLatestTask {
        @BeforeEach
        void beforeEach() {
            getLatestStatusResponse = mock(ResponseEntity.class);

            when(restTemplate.getForEntity(GET_LATEST_STATUS_URL,
                SchedulerServiceClient.TaskResponse.class,
                TestConstants.VALID_JOB_KEY))
                .thenReturn(getLatestStatusResponse);
        }

        private SchedulerServiceClient.TaskResponse getTaskResponse() {
            return new SchedulerServiceClient.TaskResponse();
        }

        @Test
        void positiveTypical() {
            when(getLatestStatusResponse.getStatusCode()).thenReturn(HttpStatus.OK);
            SchedulerServiceClient.TaskResponse expectedTaskResponse = getTaskResponse();
            when(getLatestStatusResponse.getBody()).thenReturn(expectedTaskResponse);

            SchedulerServiceClient.TaskResponse taskResponse =
                schedulerServiceClient.getLatestTask(TestConstants.VALID_JOB_KEY);
            assertEquals(expectedTaskResponse, taskResponse, "TaskResponse should match");

            verify(restTemplate, times(1)).getForEntity(GET_LATEST_STATUS_URL,
                SchedulerServiceClient.TaskResponse.class,
                TestConstants.VALID_JOB_KEY);
        }

        @Test
        void negativeNoJobKey() {
            schedulerServiceClient.getLatestTask(
                null);
            verifyNoInteractions(restTemplate);
        }

        @Test
        void negativeBlankJobKey() {
            schedulerServiceClient.getLatestTask(
                "");
            verifyNoInteractions(restTemplate);
        }

        @Test
        void negativeNotFound() {
            when(getLatestStatusResponse.getStatusCode()).thenReturn(HttpStatus.NOT_FOUND);
            when(getLatestStatusResponse.getBody()).thenReturn(getTaskResponse());

            SchedulerServiceClient.TaskResponse taskResponse =
                schedulerServiceClient.getLatestTask(TestConstants.VALID_JOB_KEY);
            assertNull(taskResponse, "TaskResponse should be null");

            verify(restTemplate, times(1)).getForEntity(GET_LATEST_STATUS_URL,
                SchedulerServiceClient.TaskResponse.class,
                TestConstants.VALID_JOB_KEY);
        }

        @Test
        void negativeWrongStatusCode() {
            when(getLatestStatusResponse.getStatusCode()).thenReturn(HttpStatus.BAD_REQUEST);
            RemoteGatewayException remoteGatewayException = assertThrows(RemoteGatewayException.class,
                () -> schedulerServiceClient.getLatestTask(TestConstants.VALID_JOB_KEY),
                "Should throw RemoteGatewayException");
            assertEquals("Call to SchedulerServiceClient.getLatestTask(String jobKey) failed status code "
                    + "was: 400 BAD_REQUEST",
                remoteGatewayException.getMessage(),
                "Exception message should match");
        }

        @Test
        void negativeUnexpectedException() {
            RuntimeException cause = new RuntimeException("I am the cause");

            when(restTemplate.getForEntity(GET_LATEST_STATUS_URL,
                SchedulerServiceClient.TaskResponse.class,
                TestConstants.VALID_JOB_KEY))
                .thenThrow(cause);

            InternalServerException internalServerException = assertThrows(InternalServerException.class,
                () -> schedulerServiceClient.getLatestTask(TestConstants.VALID_JOB_KEY),
                "Should throw InternalServerException");
            assertEquals("Failed to get latest Job status",
                internalServerException.getMessage(),
                "Exception message should match");
            assertEquals(cause, internalServerException.getCause(), "Cause should match");
        }

    }

    @DisplayName("GetTask: " + GET_STATUS_URL)
    @Nested
    class GetTask {
        @BeforeEach
        void beforeEach() {
            getStatusResponse = mock(ResponseEntity.class);

            when(restTemplate.getForEntity(GET_STATUS_URL,
                SchedulerServiceClient.TaskResponse.class,
                TestConstants.VALID_JOB_KEY,
                TestConstants.VALID_TASK_ID_LONG))
                .thenReturn(getStatusResponse);
        }


        private SchedulerServiceClient.TaskResponse getTaskResponse() {
            return new SchedulerServiceClient.TaskResponse();
        }

        @Test
        void positiveTypical() {
            when(getStatusResponse.getStatusCode()).thenReturn(HttpStatus.OK);
            SchedulerServiceClient.TaskResponse expectedTaskResponse = getTaskResponse();
            when(getStatusResponse.getBody()).thenReturn(expectedTaskResponse);

            SchedulerServiceClient.TaskResponse taskResponse =
                schedulerServiceClient.getTask(TestConstants.VALID_JOB_KEY, TestConstants.VALID_TASK_ID_LONG);

            verify(restTemplate, times(1)).getForEntity(GET_STATUS_URL,
                SchedulerServiceClient.TaskResponse.class,
                TestConstants.VALID_JOB_KEY,
                TestConstants.VALID_TASK_ID_LONG);
            assertEquals(expectedTaskResponse, taskResponse, "TaskResponse should match");
        }

        @Test
        void negativeNoJobKey() {
            schedulerServiceClient.getTask(
                "", TestConstants.VALID_TASK_ID_LONG);
            verifyNoInteractions(restTemplate);

        }

        @Test
        void negativeBlankJobKey() {
            schedulerServiceClient.getTask(
                "", TestConstants.VALID_TASK_ID_LONG);
            verifyNoInteractions(restTemplate);

        }

        @Test
        void negativeNoTaskId() {
            schedulerServiceClient.getTask(
                TestConstants.VALID_JOB_KEY, null);
            verifyNoInteractions(restTemplate);
        }

        @Test
        void negativeNotFound() {
            when(getStatusResponse.getStatusCode()).thenReturn(HttpStatus.NOT_FOUND);
            SchedulerServiceClient.TaskResponse expectedTaskResponse = getTaskResponse();
            when(getStatusResponse.getBody()).thenReturn(expectedTaskResponse);

            SchedulerServiceClient.TaskResponse taskResponse =
                schedulerServiceClient.getTask(TestConstants.VALID_JOB_KEY, TestConstants.VALID_TASK_ID_LONG);

            verify(restTemplate, times(1)).getForEntity(GET_STATUS_URL,
                SchedulerServiceClient.TaskResponse.class,
                TestConstants.VALID_JOB_KEY,
                TestConstants.VALID_TASK_ID_LONG);
            assertNull(taskResponse, "TaskResponse should be null");
        }


        @Test
        void negativeWrongStatusCode() {
            when(getStatusResponse.getStatusCode()).thenReturn(HttpStatus.BAD_REQUEST);
            RemoteGatewayException remoteGatewayException = assertThrows(RemoteGatewayException.class,
                () -> schedulerServiceClient.getTask(TestConstants.VALID_JOB_KEY, TestConstants.VALID_TASK_ID_LONG),
                "Should throw RemoteGatewayException");
            assertEquals("Call to SchedulerServiceClient.getTask(String jobKey,Long taskId) failed status code "
                    + "was: 400 BAD_REQUEST",
                remoteGatewayException.getMessage(),
                "Exception message should match");
        }

        @Test
        void negativeUnexpectedException() {
            RuntimeException cause = new RuntimeException("I am the cause");

            when(restTemplate.getForEntity(GET_STATUS_URL,
                SchedulerServiceClient.TaskResponse.class,
                TestConstants.VALID_JOB_KEY,
                TestConstants.VALID_TASK_ID_LONG))
                .thenThrow(cause);

            InternalServerException internalServerException = assertThrows(InternalServerException.class,
                () -> schedulerServiceClient.getTask(TestConstants.VALID_JOB_KEY, TestConstants.VALID_TASK_ID_LONG),
                "Should throw InternalServerException");
            assertEquals("Failed to get latest Job status",
                internalServerException.getMessage(),
                "Exception message should match");
            assertEquals(cause, internalServerException.getCause(), "Cause should match");
        }
    }
}
