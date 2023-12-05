package uk.gov.hmcts.juror.job.execution.jobs;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.juror.job.execution.database.model.MetaData;
import uk.gov.hmcts.juror.job.execution.model.Status;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

@Getter
@Slf4j
public abstract class ParallelJob extends Job {


    protected ParallelJob() {
    }

    public abstract List<ResultSupplier> getResultSuppliers();

    @Override
    public Result executeRunners(MetaData metaData) {
        List<Result> results = Collections.synchronizedList(new ArrayList<>());

        for (ResultSupplier resultSupplier : getResultSuppliers()) {
            AtomicBoolean hasError = new AtomicBoolean(false);
            List<Result> resultSupplierResults = Collections.synchronizedList(new ArrayList<>());
            resultSupplier.getResultRunners().parallelStream().forEach(resultRunner -> {
                Result result = runJobStep(resultRunner, metaData);
                if (result.getStatus() != Status.SUCCESS) {
                    hasError.set(true);
                }
                resultSupplierResults.add(result);
            });
            Result result = Result.merge(resultSupplierResults);
            results.add(result);
            resultSupplier.runPostActions(result);
            if (!resultSupplier.isContinueOnFailure() && hasError.get()) {
                break;
            }
        }
        return Result.merge(results);
    }
}
