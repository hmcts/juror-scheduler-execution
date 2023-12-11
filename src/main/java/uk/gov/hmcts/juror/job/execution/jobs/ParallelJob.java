package uk.gov.hmcts.juror.job.execution.jobs;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.juror.job.execution.database.model.MetaData;
import uk.gov.hmcts.juror.job.execution.model.Status;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
@Slf4j
public abstract class ParallelJob extends Job {


    protected ParallelJob() {
        super();
    }

    public abstract List<ResultSupplier> getResultSuppliers();

    @Override
    public Result executeRunners(MetaData metaData) {
        List<Result> results = Collections.synchronizedList(new ArrayList<>());

        for (ResultSupplier resultSupplier : getResultSuppliers()) {
            Result result = executeResultSupplier(resultSupplier, metaData);
            results.add(result);
            if (!resultSupplier.isContinueOnFailure() && result.getStatus() != Status.SUCCESS) {
                break;
            }
        }
        return Result.merge(results);
    }

    private Result executeResultSupplier(ResultSupplier resultSupplier, MetaData metaData) {
        List<Result> resultSupplierResults = Collections.synchronizedList(new ArrayList<>());
        resultSupplier.getResultRunners().parallelStream().forEach(resultRunner ->
            resultSupplierResults.add(runJobStep(resultRunner, metaData)));
        Result result = Result.merge(resultSupplierResults);
        resultSupplier.runPostActions(result);
        return result;
    }
}
