package uk.gov.hmcts.juror.job.execution.client.contracts;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.validator.constraints.Length;
import uk.gov.hmcts.juror.job.execution.model.Status;
import uk.gov.hmcts.juror.standard.client.contract.Client;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public interface SchedulerServiceClient extends Client {

    void updateStatus(String jobKey, Long taskId, StatusUpdatePayload payload);

    TaskResponse getLatestTask(String jobKey);

    TaskResponse getTask(String jobKey, Long taskId);


    @Getter
    @Setter
    class TaskResponse {

        @JsonProperty("job_key")
        private String jobKey;

        @JsonProperty("task_id")
        private int taskId;

        @JsonProperty("created_at")
        private LocalDateTime createdAt;

        @JsonProperty("last_updated_at")
        private LocalDateTime lastUpdatedAt;
        @NotNull
        private Status status;

        private String message;

        @JsonProperty("meta_data")
        private Map<String, String> metaData;

        public Map<String, String> getMetaData() {
            if (this.metaData == null) {
                return Collections.emptyMap();
            }
            return Collections.unmodifiableMap(this.metaData);
        }
    }

    @AllArgsConstructor
    @ToString
    @Getter
    class StatusUpdatePayload {
        @Setter
        @NotNull
        private Status status;

        @Length(min = 1, max = 2500)
        @Setter
        private String message;

        @JsonProperty("meta_data")
        private Map<
            @Length(min = 1, max = 2500) String,
            @Length(min = 1, max = 2500) String> metaData;

        private Map<String, String> getMetaDataInternal() {
            if (this.metaData == null) {
                this.metaData = new ConcurrentHashMap<>();
            }
            return this.metaData;
        }

        public void addMetaData(String key, String value) {
            this.getMetaDataInternal().put(key, value);
        }

        public String getMetaDataValue(String key) {
            return getMetaDataInternal().get(key);
        }

        public String getMetaDataValueOrDefault(String key, String defaultValue) {
            return getMetaDataInternal().getOrDefault(key, defaultValue);
        }
    }
}
