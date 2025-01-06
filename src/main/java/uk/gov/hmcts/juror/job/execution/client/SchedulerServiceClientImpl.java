package uk.gov.hmcts.juror.job.execution.client;

import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.juror.job.execution.client.contracts.SchedulerServiceClient;
import uk.gov.hmcts.juror.standard.client.AbstractRemoteRestClient;
import uk.gov.hmcts.juror.standard.client.contract.ClientType;
import uk.gov.hmcts.juror.standard.service.exceptions.InternalServerException;
import uk.gov.hmcts.juror.standard.service.exceptions.RemoteGatewayException;

import java.time.LocalDateTime;

import static uk.gov.hmcts.juror.job.execution.jobs.dashboard.ams.data.Timestamps.DATE_TIME_FORMATTER;

@Slf4j
@Component
public class SchedulerServiceClientImpl extends AbstractRemoteRestClient implements SchedulerServiceClient {

    private final String updateStatusUrl;
    private final String getLatestStatusUrl;
    private final String getStatusUrl;

    public SchedulerServiceClientImpl(
        @ClientType("SchedulerService") RestTemplateBuilder restTemplateBuilder,
        @Value("${uk.gov.hmcts.juror.job.execution.remote.scheduler-service.scheme}") String scheme,
        @Value("${uk.gov.hmcts.juror.job.execution.remote.scheduler-service.host}") String host,
        @Value("${uk.gov.hmcts.juror.job.execution.remote.scheduler-service.port}") String port,
        @Value("${uk.gov.hmcts.juror.job.execution.remote.scheduler-service-update-status-url}") String updateStatusUrl,
        @Value("${uk.gov.hmcts.juror.job.execution.remote.scheduler-service-get-latest-status-url}")
        String getLatestStatusUrl,
        @Value("${uk.gov.hmcts.juror.job.execution.remote.scheduler-service-get-status-url}") String getStatusUrl
    ) {
        super(restTemplateBuilder);
        String urlPrefix = scheme + "://" + host + ":" + port;
        this.updateStatusUrl = urlPrefix + updateStatusUrl;
        this.getLatestStatusUrl = urlPrefix + getLatestStatusUrl;
        this.getStatusUrl = urlPrefix + getStatusUrl;
    }

    @Override
    public void updateStatus(String jobKey, Long taskId, StatusUpdatePayload payload) {
        ResponseEntity<Void> response;
        try {
            log.info(
                "[JobKey: {}]\n[{}]\nTaskId: {},\nstatus={},\nmessage={},\nmetadata={}",
                jobKey,
                DATE_TIME_FORMATTER.format(LocalDateTime.now()),
                taskId,
                payload.getStatus(),
                payload.getMessage(),
                payload.getMetaData()
            );
            if (Strings.isBlank(jobKey) || taskId == null) {
                return;//No need to continue if jobKey/taskId are not provided as these are required for reporting back
            }
            HttpEntity<StatusUpdatePayload> requestUpdate = new HttpEntity<>(payload);

            response =
                restTemplate.exchange(updateStatusUrl, HttpMethod.PUT, requestUpdate, Void.class, jobKey, taskId);
        } catch (Exception exception) {
            String message = "Failed to upload Job result";
            log.error(message, exception);
            throw new InternalServerException(message, exception);
        }

        final HttpStatusCode statusCode = response.getStatusCode();
        if (!statusCode.equals(HttpStatus.ACCEPTED)) {
            throw new RemoteGatewayException(
                "Call to SchedulerServiceClient.updateStatus(jobKey, taskId, payload) failed status code"
                    + " was: "
                    + statusCode);
        }
    }

    @Override
    public TaskResponse getLatestTask(String jobKey) {
        ResponseEntity<TaskResponse> response;
        try {
            if (Strings.isBlank(jobKey)) {
                return null;
            }
            log.debug("Getting Job Status for JobKey: " + jobKey);
            response =
                restTemplate.getForEntity(getLatestStatusUrl, TaskResponse.class, jobKey);
        } catch (Exception exception) {
            String message = "Failed to get latest Job status";
            log.error(message, exception);
            throw new InternalServerException(message, exception);
        }

        final HttpStatusCode statusCode = response.getStatusCode();
        if (statusCode.equals(HttpStatus.NOT_FOUND)) {
            return null;
        }
        if (!statusCode.equals(HttpStatus.OK)) {
            throw new RemoteGatewayException(
                "Call to SchedulerServiceClient.getLatestTask(String jobKey) failed status code was: "
                    + statusCode);
        }
        return response.getBody();
    }

    @Override
    public TaskResponse getTask(String jobKey, Long taskId) {
        ResponseEntity<TaskResponse> response;
        try {
            if (Strings.isBlank(jobKey) || taskId == null) {
                return null;
            }
            log.debug("Getting Job Status for JobKey: " + jobKey + " TaskId: " + taskId);
            response =
                restTemplate.getForEntity(getStatusUrl, TaskResponse.class, jobKey, taskId);
        } catch (Exception exception) {
            String message = "Failed to get latest Job status";
            log.error(message, exception);
            throw new InternalServerException(message, exception);
        }

        final HttpStatusCode statusCode = response.getStatusCode();
        if (statusCode.equals(HttpStatus.NOT_FOUND)) {
            return null;
        }
        if (!statusCode.equals(HttpStatus.OK)) {
            throw new RemoteGatewayException(
                "Call to SchedulerServiceClient.getTask(String jobKey,Long taskId) failed status code was: "
                    + statusCode);
        }
        return response.getBody();
    }
}
