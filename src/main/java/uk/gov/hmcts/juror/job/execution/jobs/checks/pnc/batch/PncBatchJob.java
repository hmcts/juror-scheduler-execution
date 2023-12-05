package uk.gov.hmcts.juror.job.execution.jobs.checks.pnc.batch;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.juror.job.execution.client.contracts.JurorServiceClient;
import uk.gov.hmcts.juror.job.execution.client.contracts.PoliceNationalCheckServiceClient;
import uk.gov.hmcts.juror.job.execution.client.contracts.SchedulerServiceClient;
import uk.gov.hmcts.juror.job.execution.database.model.MetaData;
import uk.gov.hmcts.juror.job.execution.database.model.RequirePncCheck;
import uk.gov.hmcts.juror.job.execution.jobs.LinearJob;
import uk.gov.hmcts.juror.job.execution.model.Status;
import uk.gov.hmcts.juror.job.execution.service.contracts.DatabaseService;
import uk.gov.hmcts.juror.standard.Utilities;
import uk.gov.hmcts.juror.standard.service.exceptions.InternalServerException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Component
@Slf4j
public class PncBatchJob extends LinearJob {
    public static final String TOTAL_CHECKS_REQUESTED_KEY;
    public static final String TOTAL_CHECKS_IN_BATCH_KEY;
    public static final String TOTAL_BATCHES_RESPONSES_KEY;
    public static final String TOTAL_BATCHES_REQUESTED_KEY;

    static final Set<String> META_DATA_KEYS_TO_COMBINE;
    static final String GET_JURORS_THAT_REQUIRE_PNC_CHECK_SQL;

    private final DatabaseService databaseService;
    private final PncBatchConfig config;
    private final PoliceNationalCheckServiceClient policeNationalCheckServiceClient;
    private final SchedulerServiceClient schedulerServiceClient;
    private final JurorServiceClient jurorServiceClient;


    static {
        GET_JURORS_THAT_REQUIRE_PNC_CHECK_SQL = "select * from juror_mod.require_pnc_check_view";
        TOTAL_CHECKS_REQUESTED_KEY = "TOTAL_CHECKS_REQUESTED";
        TOTAL_CHECKS_IN_BATCH_KEY = "TOTAL_CHECKS_IN_BATCH";
        TOTAL_BATCHES_RESPONSES_KEY = "TOTAL_BATCHES_RESPONSES";
        TOTAL_BATCHES_REQUESTED_KEY = "TOTAL_BATCHES_REQUESTED";

        HashSet<String> metaDataKeys = new HashSet<>();
        for (PoliceCheck policeCheck : PoliceCheck.values()) {
            metaDataKeys.add("TOTAL_WITH_STATUS_" + policeCheck.name());
        }
        metaDataKeys.add(TOTAL_CHECKS_REQUESTED_KEY);
        metaDataKeys.add(TOTAL_CHECKS_IN_BATCH_KEY);
        metaDataKeys.add("TOTAL_NULL_RESULTS");
        META_DATA_KEYS_TO_COMBINE = Collections.unmodifiableSet(metaDataKeys);
    }

    public PncBatchJob(DatabaseService databaseService, PncBatchConfig pncBatchConfig,
                       PoliceNationalCheckServiceClient policeNationalCheckServiceClient,
                       SchedulerServiceClient schedulerServiceClient,
                       JurorServiceClient jurorServiceClient) {
        this.databaseService = databaseService;
        this.config = pncBatchConfig;
        this.policeNationalCheckServiceClient = policeNationalCheckServiceClient;
        this.schedulerServiceClient = schedulerServiceClient;
        this.jurorServiceClient = jurorServiceClient;
    }

    @Override
    public ResultSupplier getResultSupplier() {
        return new ResultSupplier(false, List.of(metaData -> {
            List<PoliceNationalCheckServiceClient.JurorCheckRequest> jurorCheckRequests = getJurorsToCheck();
            if (jurorCheckRequests.isEmpty()) {
                return new Result(Status.SUCCESS,
                    "0 jurors need PNC checks. As such none were sent to the PNC service", null)
                    .addMetaData(TOTAL_BATCHES_REQUESTED_KEY, "0")
                    .addMetaData(TOTAL_CHECKS_REQUESTED_KEY, "0");
            }
            int numberOfBatches = triggerPncCheck(jurorCheckRequests, metaData);
            return new Result(Status.PROCESSING,
                jurorCheckRequests.size() + " jurors sent to the PNC service to be checked. "
                    + "In " + numberOfBatches + " batches",
                null)
                .addMetaData(TOTAL_BATCHES_REQUESTED_KEY, String.valueOf(numberOfBatches))
                .addMetaData(TOTAL_CHECKS_REQUESTED_KEY, String.valueOf(jurorCheckRequests.size()));
        }));
    }


    public int triggerPncCheck(List<PoliceNationalCheckServiceClient.JurorCheckRequest> jurorCheckRequests,
                               MetaData metaData) {
        List<List<PoliceNationalCheckServiceClient.JurorCheckRequest>> batches =
            Utilities.getBatches(jurorCheckRequests, config.getBatchSize());
        for (List<PoliceNationalCheckServiceClient.JurorCheckRequest> batch : batches) {
            this.policeNationalCheckServiceClient.checkJurors(
                PoliceNationalCheckServiceClient.JurorCheckRequestBulk.builder().checks(batch).metaData(
                    new PoliceNationalCheckServiceClient.JurorCheckRequestBulk.MetaData(metaData.getJobKey(),
                        metaData.getTaskId())).build());
        }
        return batches.size();
    }

    List<PoliceNationalCheckServiceClient.JurorCheckRequest> getJurorsToCheck() {
        List<RequirePncCheck> jurorsThatRequirePncCheck = new ArrayList<>();
        //Get all jurors that require a PNC check
        databaseService.execute(config.getDatabase(), connection -> {
            log.info("Getting jurors that require PNC checks");
            jurorsThatRequirePncCheck.addAll(
                databaseService.executePreparedStatement(connection, RequirePncCheck.class,
                    GET_JURORS_THAT_REQUIRE_PNC_CHECK_SQL));
        });

        //Process the jurors that require a PNC check
        List<PoliceNationalCheckServiceClient.JurorCheckRequest> jurorCheckRequests = new ArrayList<>();
        jurorsThatRequirePncCheck.forEach(requirePncCheck -> {
            //If the user is missing information update status and skip
            if (StringUtils.isBlank(requirePncCheck.getPostcode())
                || requirePncCheck.getDateOfBirth() == null) {
                jurorServiceClient.call(requirePncCheck.getJurorNumber(),
                    new JurorServiceClient.Payload(PoliceCheck.INSUFFICIENT_INFORMATION));
                log.info("Skipping juror {} as they are missing information", requirePncCheck.getJurorNumber());
                return;
            }
            //If this is the users first check update status to in progress and continue
            if (requirePncCheck.getPoliceCheck() == null
                || requirePncCheck.getPoliceCheck() == PoliceCheck.NOT_CHECKED) {
                jurorServiceClient.call(requirePncCheck.getJurorNumber(),
                    new JurorServiceClient.Payload(PoliceCheck.IN_PROGRESS));
            }
            jurorCheckRequests.add(requirePncCheck.toJurorCheckRequest());
        });
        return jurorCheckRequests;
    }


    public void updateResult(SchedulerServiceClient.StatusUpdatePayload payload, String jobKey, Long taskId) {
        if (payload != null) {
            SchedulerServiceClient.TaskResponse taskResponse = this.schedulerServiceClient.getTask(jobKey, taskId);
            if (taskResponse == null) {
                throw new InternalServerException(
                    "Task with jobKey: " + jobKey + " taskId: " + taskId + " was not found. "
                        + "Failed to update status");
            }
            combineMetaData(payload, taskResponse);

            payload.addMetaData(TOTAL_BATCHES_RESPONSES_KEY,
                combineMetaData(taskResponse.getMetaData().get(TOTAL_BATCHES_RESPONSES_KEY), "1"));

            if (payload.getMetaDataValue(TOTAL_BATCHES_RESPONSES_KEY).equals(taskResponse.getMetaData().get(
                TOTAL_BATCHES_REQUESTED_KEY))) {

                Long totalChecksRequested = Long.parseLong(payload.getMetaDataValue(TOTAL_CHECKS_REQUESTED_KEY));
                if (totalChecksRequested.equals(sumMetaData(payload, PoliceCheck.ELIGIBLE,
                    PoliceCheck.INELIGIBLE))) {
                    payload.setStatus(Status.SUCCESS);
                    payload.setMessage("All batches have successfully processed");
                } else {
                    payload.setStatus(Status.PARTIAL_SUCCESS);
                    payload.setMessage("All batches have processed but some checks did not process");
                }
            }
            this.schedulerServiceClient.updateStatus(jobKey, taskId, payload);
        }
    }

    void combineMetaData(SchedulerServiceClient.StatusUpdatePayload payload,
                         SchedulerServiceClient.TaskResponse taskResponse) {
        for (String key : META_DATA_KEYS_TO_COMBINE) {
            if (taskResponse == null) {
                payload.addMetaData(key, payload.getMetaDataValueOrDefault(key,"0"));
            } else {
                payload.addMetaData(key,
                    combineMetaData(taskResponse.getMetaData().get(key), payload.getMetaDataValue(key)));
            }
        }
    }

    String combineMetaData(String oldValue, String newValue) {
        if (oldValue == null && newValue != null) {
            return newValue;
        }
        if (oldValue != null && newValue == null) {
            return oldValue;
        }
        if (oldValue == null) {
            return "0";
        }
        if (StringUtils.isNumeric(oldValue)) {
            if (StringUtils.isNumeric(newValue)) {
                long oldValueNumeric = Long.parseLong(oldValue);
                long newValueNumeric = Long.parseLong(newValue);
                return String.valueOf(oldValueNumeric + newValueNumeric);
            } else {
                return oldValue;
            }
        } else {
            return newValue;
        }
    }

    long sumMetaData(SchedulerServiceClient.StatusUpdatePayload payload, PoliceCheck... policeChecks) {
        long value = 0;
        for (PoliceCheck policeCheck : policeChecks) {
            value += Long.parseLong(payload.getMetaDataValue("TOTAL_WITH_STATUS_" + policeCheck.name()));
        }
        return value;
    }
}
