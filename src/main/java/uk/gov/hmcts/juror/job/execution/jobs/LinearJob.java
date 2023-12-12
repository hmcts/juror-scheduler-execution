package uk.gov.hmcts.juror.job.execution.jobs;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.juror.job.execution.database.model.MetaData;
import uk.gov.hmcts.juror.job.execution.model.Status;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@Getter
@Slf4j
public abstract class LinearJob extends Job {

    protected LinearJob() {
        super();
    }


    @Override
    public Result executeRunners(MetaData metaData) {
        List<Result> results = new ArrayList<>();
        ResultSupplier resultSupplier = getResultSupplier();

        for (Function<MetaData, Result> resultRunner : resultSupplier.getResultRunners()) {
            Result result = runJobStep(resultRunner, metaData);
            results.add(result);
            if (!resultSupplier.isContinueOnFailure()
                && result.getStatus() != Status.SUCCESS) {
                break;
            }
        }
        Result result = Result.merge(results);
        resultSupplier.runPostActions(result);
        return result;
    }

    public abstract ResultSupplier getResultSupplier();
}
