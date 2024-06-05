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
        select tfa.form_type type,
                    (case when tfa.form_type ='5221' then 'SUMMONS ENGLISH'
                    when tfa.form_type ='5221C' then 'SUMMONS BI-LINGUAL'
                    when tfa.form_type ='5224' then 'WITHDRAWAL ENGLISH'
                    when tfa.form_type ='5224A' then 'CONFIRMATION ENGLISH'
                    when tfa.form_type ='5224AC' then 'CONFIRMATION WELSH'
                    when tfa.form_type ='5224C' then 'WITHDRAWAL WELSH'
                    when tfa.form_type ='5225' then 'EXCUSAL ENGLISH'
                    when tfa.form_type ='5225C' then 'EXCUSAL WELSH'
                    when tfa.form_type ='5226' then 'NON-EXCUSAL ENGLISH'
                    when tfa.form_type ='5226A' then 'NON-DEFER ENGLISH'
                    when tfa.form_type ='5226AC' then 'NON-DEFER WELSH'
                    when tfa.form_type ='5226C' then 'NON-EXCUSAL WELSH'
                    when tfa.form_type ='5227' then 'REQUEST ENGLISH'
                    when tfa.form_type ='5227C' then 'REQUEST WELSH'
                    when tfa.form_type ='5228' then 'NON-RESPONDED ENGLISH'
                    when tfa.form_type ='5228C' then 'NON-RESPONDED WELSH'
                    when tfa.form_type ='5229' then 'POSTPONE ENGLISH'
                    when tfa.form_type ='5229A' then 'DEFERRED ENGLISH'
                    when tfa.form_type ='5229AC' then 'DEFERRED WELSH'
                    when tfa.form_type ='5229C' then 'POSTPONE WELSH' else 'UNKNOWN' end) description,
                    coalesce(sum(form_counts.number_of_items),0) "count"
                    from juror_mod.t_form_attr tfa left outer join (
                        SELECT form_type,
                        count(juror_no) AS number_of_items
                        FROM juror_mod.bulk_print_data bpd
                        where creation_date >= current_date - case when to_char(current_date, 'dy') = 'tue' then 3 else 1 end
                        GROUP BY form_type, (to_date(creation_date::text, 'YYYY-MM-DD'::text))) form_counts
                    on tfa.form_type = form_counts.form_type
                    group by tfa.form_type, tfa.dir_name
                    order by 1;
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
