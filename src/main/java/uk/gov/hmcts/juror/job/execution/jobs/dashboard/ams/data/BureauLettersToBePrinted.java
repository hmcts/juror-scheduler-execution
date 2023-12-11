package uk.gov.hmcts.juror.job.execution.jobs.dashboard.ams.data;

import uk.gov.hmcts.juror.job.execution.config.DatabaseConfig;
import uk.gov.hmcts.juror.job.execution.jobs.Job;
import uk.gov.hmcts.juror.job.execution.service.contracts.DatabaseService;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("PMD.LawOfDemeter")
public class BureauLettersToBePrinted extends DashboardDataEntry {
    public static final String BUREAU_LETTERS_TO_BE_PRINTED_SQL = """
            select f.form_type type,
            decode(f.form_type,
            '5221','SUMMONS ENGLISH',
            '5221C','SUMMONS BI-LINGUAL',
            '5224','WITHDRAWAL ENGLISH',
            '5224A','CONFIRMATION ENGLISH',
            '5224AC','CONFIRMATION WELSH',
            '5224C','WITHDRAWAL WELSH',
            '5225','EXCUSAL ENGLISH',
            '5225C','EXCUSAL WELSH',
            '5226','NON-EXCUSAL ENGLISH',
            '5226A','NON-DEFER ENGLISH',
            '5226AC','NON-DEFER WELSH',
            '5226C','NON-EXCUSAL WELSH',
            '5227','REQUEST ENGLISH',
            '5227C','REQUEST WELSH',
            '5228','NON-RESPONDED ENGLISH',
            '5228C','NON-RESPONDED WELSH',
            '5229','POSTPONE ENGLISH',
            '5229A','DEFERRED ENGLISH',
            '5229AC','DEFERRED WELSH',
            '5229C','POSTPONE WELSH',
            'UNKNOWN') description,
            sum(nvl(abc.number_of_items,0)) count
            from form_attr f left outer join (
            Select form_type, number_of_items  from abaccus a
            where creation_date < trunc(sysdate) and
            creation_date >= trunc(sysdate-decode(TO_CHAR(sysdate,'dy'), 'tue', 3,1))) abc
            on f.form_type = abc.form_type
            group by f.form_type, f.dir_name
            order by 1
        """;
    final DatabaseService databaseService;
    final DatabaseConfig databaseConfig;
    final Clock clock;

    protected BureauLettersToBePrinted(DashboardData dashboardData,
                                       DatabaseService databaseService,
                                       DatabaseConfig databaseConfig,
                                       Clock clock) {
        super(dashboardData, "Bureau Letters To Be Printed", "Type", "Description", "Count");
        this.databaseService = databaseService;
        this.databaseConfig = databaseConfig;
        this.clock = clock;
    }

    public void addRow(String type, String description, String count) {
        addEntry(type, description, count);
    }

    public void addRow(BureauLettersToBePrintedDB bureauLettersToBePrintedDB) {
        this.addRow(bureauLettersToBePrintedDB.getType(),
            bureauLettersToBePrintedDB.getDescription(),
            String.valueOf(bureauLettersToBePrintedDB.getCount()));
    }


    public Job.Result populate() {
        final String errorText = "ERROR";
        AtomicReference<Job.Result> result = new AtomicReference<>(null);
        try {
            databaseService.execute(databaseConfig, connection -> {
                List<BureauLettersToBePrintedDB> response =
                    databaseService.executePreparedStatement(connection, BureauLettersToBePrintedDB.class,
                        BUREAU_LETTERS_TO_BE_PRINTED_SQL);

                if (response == null || response.isEmpty()) {
                    addRow(errorText, errorText, errorText);
                    result.set(Job.Result.failed("No response from database"));
                } else {
                    response.forEach(this::addRow);
                }
            });
        } catch (Exception e) {
            addRow(errorText, errorText, errorText);
            result.set(Job.Result.failed("Unexpected exception", e));
        }
        populateTimestamp(dashboardData, "Bureau Letters To Be Printed", LocalDateTime.now(clock));
        if (result.get() == null) {
            return Job.Result.passed();
        } else {
            return result.get();
        }
    }
}
