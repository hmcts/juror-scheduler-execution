package uk.gov.hmcts.juror.job.execution.jobs.housekeeping.standard;

import lombok.Getter;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.juror.job.execution.jobs.LinearJob;
import uk.gov.hmcts.juror.job.execution.model.Status;
import uk.gov.hmcts.juror.job.execution.service.contracts.DatabaseService;

import java.sql.Types;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Component
@Getter
public class HouseKeepingJob extends LinearJob {
    private final DatabaseService databaseService;
    private final HouseKeepingConfig config;

    private final String[] resultMessages = {
        "POOL ERRORS RAISED",
        "FATAL ERROR RAISED",
        "LOG FILE ERROR RAISED",
        "PARAMETER ERROR RAISED",
        "MAX DELETION ERROR RAISED",
        "TIMEOUT ERROR RAISED"
    };

    protected HouseKeepingJob(DatabaseService databaseService, HouseKeepingConfig config) {
        super();
        this.databaseService = databaseService;
        this.config = config;
    }

    @Override
    public ResultSupplier getResultSupplier() {
        return new ResultSupplier(false,
            List.of(
                metaDate -> callHouseKeepingProcedure()
            )
        );
    }


    @SuppressWarnings("PMD.AvoidLiteralsInIfCondition")
    Result callHouseKeepingProcedure() {
        try {
            AtomicReference<Status> status = new AtomicReference<>();
            AtomicReference<String> message = new AtomicReference<>(null);
            databaseService.execute(config.getDatabase(), connection -> {
                int resultCode = databaseService.executeStoredProcedureWithReturn(connection,
                    "HOUSEKEEPING.INITIATE_RUN",
                    Integer.class,
                    Types.INTEGER,
                    Boolean.TRUE.equals(config.getOwnerRestrict())
                        ? "YES"
                        : "NO",
                    config.getReadOnly(),
                    config.getMaxRuntime());

                if (resultCode == 1) {
                    status.set(Status.SUCCESS);
                } else if (resultCode <= 6) {
                    status.set(Status.FAILED);
                    message.set(resultMessages[resultCode - 1]);
                } else {
                    status.set(Status.FAILED);
                    message.set("Unknown result code: " + resultCode);
                }
            });
            return new Result(status.get(), message.get(), null);
        } catch (Exception exception) {
            return Result.failed(exception.getMessage());
        }
    }
}
