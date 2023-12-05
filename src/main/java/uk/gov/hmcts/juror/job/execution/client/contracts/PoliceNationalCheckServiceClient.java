package uk.gov.hmcts.juror.job.execution.client.contracts;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import uk.gov.hmcts.juror.standard.client.contract.Client;

import java.time.format.DateTimeFormatter;
import java.util.List;

public interface PoliceNationalCheckServiceClient extends Client {


    DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    void checkJurors(JurorCheckRequestBulk jurorCheckRequestBulk);


    @Data
    @Builder
    class JurorCheckRequestBulk {

        @JsonProperty("meta_data")
        @Valid
        private MetaData metaData;

        @NotEmpty
        private List<@Valid JurorCheckRequest> checks;


        @AllArgsConstructor
        @Data
        public static class MetaData {
            @JsonProperty("job_key")
            @NotBlank
            private String jobKey;
            @JsonProperty("task_id")
            @NotNull
            private Long taskId;
        }
    }
    @Data
    @Builder
    class NameDetails {
        @JsonProperty("first_name")
        @NotBlank
        private String firstName;

        @JsonProperty("middle_name")
        private String middleName;

        @JsonProperty("last_name")
        @NotBlank
        private String lastName;
    }
    @Builder
    @Getter
    @EqualsAndHashCode
    class JurorCheckRequest {

        @JsonProperty("juror_number")
        @NotBlank
        @Pattern(regexp = "^\\d{9}$")
        private String jurorNumber;

        @JsonProperty("date_of_birth")
        @Pattern(regexp = "^[0-3][0-9]-((0[0-9])|(1[0-2]))-[0-9]{4}$")
        @NotNull
        private String dateOfBirth;

        @JsonProperty("post_code")
        @NotBlank
        @Pattern(regexp = "^[A-Z0-9 ]{5,8}$")
        private String postCode;

        @Valid
        @NotNull
        @JsonProperty("name")
        private NameDetails name;
    }
}
