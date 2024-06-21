package uk.gov.hmcts.juror.job.execution.database.model;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import uk.gov.hmcts.juror.job.execution.database.DatabaseColumn;

@Getter
@Setter
@Accessors(chain = true)
public class ContentStore {

    @DatabaseColumn(name = "REQUEST_ID", setter = "setRequestId")
    private Long requestId;
    @DatabaseColumn(name = "DOCUMENT_ID", setter = "setDocumentId")
    private String documentId;
    @DatabaseColumn(name = "DATA", setter = "setData")
    private String data;
    @DatabaseColumn(name = "FAILED_FILE_TRANSFER", setter = "setFailedFileTransfer")
    private Boolean failedFileTransfer;

    public boolean isFailedFileTransfer() {
        return failedFileTransfer != null && failedFileTransfer;
    }
}

