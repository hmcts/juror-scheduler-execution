package uk.gov.hmcts.juror.job.execution.jobs;

import io.jsonwebtoken.lang.Collections;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import uk.gov.hmcts.juror.job.execution.database.model.MetaData;
import uk.gov.hmcts.juror.job.execution.model.Status;
import uk.gov.hmcts.juror.job.execution.rules.Rule;
import uk.gov.hmcts.juror.job.execution.rules.RulesEngine;
import uk.gov.hmcts.juror.standard.service.exceptions.InternalServerException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

@Getter
@Slf4j
@SuppressWarnings("PMD.ShortClassName")
public abstract class Job {

    private final Set<Rule> rules;

    protected Job() {
        this.rules = new HashSet<>();
    }

    protected void addRules(Rule... rules) {
        this.rules.addAll(List.of(rules));
    }

    public final Result runRules() {
        List<String> failureMessages = RulesEngine.fire(this.getRules());
        if (!failureMessages.isEmpty()) {
            return new Result(Status.VALIDATION_FAILED, String.join("\\n", failureMessages));
        }
        return null;
    }

    public final Result execute(MetaData metaData) {
        try {
            Result rulesResult = runRules();
            if (rulesResult != null) {
                return rulesResult;
            }
            return executeRunners(metaData);
        } catch (Exception e) {
            log.error("Unexpected exception", e);
            return new Result(Status.FAILED_UNEXPECTED_EXCEPTION,
                "Unexpected exception raised: " + e.getClass().getName(), e);
        }
    }

    public abstract Result executeRunners(MetaData metaData);


    protected final Result runJobStep(Function<MetaData, Result> resultFunction, MetaData metaData) {
        try {
            return resultFunction.apply(metaData);
        } catch (InternalServerException exception) {
            String message = ExceptionUtils.getRootCauseMessage(exception);
            log.error("Internal Server error when executing Job: " + getName(), exception);
            return new Result(Status.FAILED_UNEXPECTED_EXCEPTION,
                "Internal Server exception raised: " + message,
                exception);
        } catch (Exception exception) {
            log.error("Unexpected error when executing Job: " + getName(), exception);
            return new Result(Status.FAILED_UNEXPECTED_EXCEPTION,
                "Unexpected exception raised: " + exception.getClass().getName(),
                exception);
        }
    }


    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Getter
    @EqualsAndHashCode
    public static class Result {
        private final Status status;
        private final String message;
        private final Throwable throwable;
        private final Map<String, String> metaData;


        public Result(Status status) {
            this(status, null);
        }

        public Result(Status status, String message) {
            this(status, message, null);
        }

        public Result(Status status, String message, Throwable throwable) {
            this.status = status;
            this.message = message;
            this.throwable = throwable;
            this.metaData = new ConcurrentHashMap<>();
        }

        public Result addMetaData(Map<String, String> metaData) {
            this.metaData.putAll(metaData);
            return this;
        }

        public Result addMetaData(String key, String value) {
            this.metaData.put(key, value);
            return this;
        }

        public static Result passed() {
            return passed(null);
        }

        public static Result passed(String message) {
            return new Result(Status.SUCCESS, message);
        }

        public static Result failed(String message) {
            return new Result(Status.FAILED, message);
        }

        public static Result failed(String message, Throwable throwable) {
            return new Result(Status.FAILED, message, throwable);
        }

        public static Result partialSuccess(String message) {
            return new Result(Status.PARTIAL_SUCCESS, message);
        }

        @SuppressWarnings({
            "PMD.AvoidLiteralsInIfCondition",
            "PMD.NullAssignment"
        })
        public static Result merge(Collection<Result> results) {
            if (results.size() == 1) {
                return results.iterator().next();
            }

            List<String> messages = new ArrayList<>();
            Status status = null;
            Throwable throwable = null;
            Map<String, String> metaData = new ConcurrentHashMap<>();
            for (Result result : results) {
                if (status == null || result.getStatus() != null
                    && result.getStatus().getPriority() > status.getPriority()) {
                    status = result.getStatus();
                }

                throwable = Optional.ofNullable(result.getThrowable()).orElse(throwable);

                if (!Collections.isEmpty(result.getMetaData())) {
                    metaData.putAll(result.getMetaData());
                }
                if (!StringUtils.isBlank(result.getMessage())) {
                    messages.add(result.getMessage());
                }
            }
            return new Result(status, messages.isEmpty() ? null : String.join("\\n", messages), throwable).addMetaData(
                metaData);
        }
    }


    @Getter
    public static class ResultSupplier {
        private final boolean continueOnFailure;
        private final Collection<Function<MetaData, Result>> resultRunners;
        private final Consumer<Result> postRunChecks;

        public ResultSupplier(boolean continueOnFailure, Collection<Function<MetaData, Result>> resultRunners) {
            this(continueOnFailure, resultRunners, null);
        }


        public ResultSupplier(boolean continueOnFailure, Collection<Function<MetaData, Result>> resultRunners,
                              Consumer<Result> postRunChecks) {
            this.continueOnFailure = continueOnFailure;
            this.resultRunners = resultRunners;
            this.postRunChecks = postRunChecks;
        }

        public void runPostActions(Result result) {
            if (postRunChecks == null) {
                return;
            }
            postRunChecks.accept(result);
        }
    }
}
