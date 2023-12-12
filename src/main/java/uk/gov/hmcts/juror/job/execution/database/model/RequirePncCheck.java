package uk.gov.hmcts.juror.job.execution.database.model;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import uk.gov.hmcts.juror.job.execution.client.contracts.PoliceNationalCheckServiceClient;
import uk.gov.hmcts.juror.job.execution.database.DatabaseColumn;
import uk.gov.hmcts.juror.job.execution.jobs.checks.pnc.batch.PoliceCheck;

import java.time.LocalDate;

@Getter
@Setter
@Accessors(chain = true)
public class RequirePncCheck {

    @DatabaseColumn(name = "police_check", setter = "setPoliceCheck")
    private PoliceCheck policeCheck;
    @DatabaseColumn(name = "juror_number", setter = "setJurorNumber")
    private String jurorNumber;
    @DatabaseColumn(name = "first_name", setter = "setFirstName")
    private String firstName;
    @DatabaseColumn(name = "middle_name", setter = "setMiddleName")
    private String middleName;
    @DatabaseColumn(name = "last_name", setter = "setLastName")
    private String lastName;
    @DatabaseColumn(name = "date_of_birth", setter = "setDateOfBirth")
    private LocalDate dateOfBirth;
    @DatabaseColumn(name = "post_code", setter = "setPostcode")
    private String postcode;


    @SuppressWarnings("PMD.LawOfDemeter")
    public PoliceNationalCheckServiceClient.JurorCheckRequest toJurorCheckRequest() {
        return PoliceNationalCheckServiceClient.JurorCheckRequest.builder()
            .jurorNumber(jurorNumber)
            .dateOfBirth(dateOfBirth.format(PoliceNationalCheckServiceClient.DATE_FORMATTER))
            .postCode(postcode)
            .name(PoliceNationalCheckServiceClient.NameDetails.builder()
                .firstName(firstName)
                .middleName(middleName)
                .lastName(lastName)
                .build())
            .build();
    }
}