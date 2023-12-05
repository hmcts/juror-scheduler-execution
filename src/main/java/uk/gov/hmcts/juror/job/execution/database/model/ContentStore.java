package uk.gov.hmcts.juror.job.execution.database.model;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import uk.gov.hmcts.juror.job.execution.database.DatabaseColumn;

import java.math.BigDecimal;

@Getter
@Setter
@Accessors(chain = true)
public class ContentStore {

    @DatabaseColumn(name = "REQUEST_ID", setter = "setRequestId")
    private BigDecimal requestId;
    @DatabaseColumn(name = "DOCUMENT_ID", setter = "setDocumentId")
    private String documentId;
    @DatabaseColumn(name = "DATA", isClob = true, setter = "setData")
    private String data;
}

