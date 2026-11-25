package uk.gov.hmcts.juror.job.execution.jobs.dashboard.ams.data;

import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.juror.job.execution.config.DatabaseConfig;
import uk.gov.hmcts.juror.job.execution.jobs.Job;
import uk.gov.hmcts.juror.job.execution.service.contracts.DatabaseService;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("PMD.LawOfDemeter")
@Slf4j
public class EmailsSent extends DashboardDataEntry {
    public static final String EMAILS_SENT_SQL = """
        select tfa.form_type type,
            (case
            when tfa.form_type ='5224A' then 'CONFIRMATION ENGLISH'
            when tfa.form_type ='5224AC' then 'CONFIRMATION WELSH'
            when tfa.form_type ='5224' then 'WITHDRAWAL ENGLISH'
            when tfa.form_type ='5224C' then 'WITHDRAWAL WELSH'
            when tfa.form_type ='5225' then 'EXCUSAL ENGLISH'
            when tfa.form_type ='5225C' then 'EXCUSAL WELSH'
            when tfa.form_type ='5226A' then 'NON-DEFER ENGLISH'
            when tfa.form_type ='5226AC' then 'NON-DEFER WELSH'
            when tfa.form_type ='5226' then 'NON-EXCUSAL ENGLISH'
            when tfa.form_type ='5226C' then 'NON-EXCUSAL WELSH'
            when tfa.form_type ='5227' then 'REQUEST ENGLISH'
            when tfa.form_type ='5227C' then 'REQUEST WELSH'
            when tfa.form_type ='5229' then 'POSTPONE ENGLISH'
            when tfa.form_type ='5229C' then 'POSTPONE WELSH'
            when tfa.form_type ='5229A' then 'DEFERRED ENGLISH'
            when tfa.form_type ='5229AC' then 'DEFERRED WELSH'
                else 'UNKNOWN' end) description,
            coalesce(sum(form_counts.number_of_items),0)::bigint "count"
        from juror_mod.t_form_attr tfa left outer join (
            select bpd.form_type,
                count(bpd.juror_no) as number_of_items
            from juror_mod.bulk_print_data bpd
            where bpd.extracted_date = current_date - 1
                and bpd.communication_channel = 'EMAIL'
            group by bpd.form_type) form_counts
        on tfa.form_type = form_counts.form_type
        where tfa.form_type in (
            '5224A', '5224AC', '5224', '5224C',
            '5225', '5225C', '5226A', '5226AC',
            '5226', '5226C', '5227', '5227C',
            '5229', '5229C', '5229A', '5229AC'
        )
        group by tfa.form_type, tfa.dir_name
        order by 1;
        """;

    final DatabaseService databaseService;
    final DatabaseConfig databaseConfig;
    final Clock clock;

    protected EmailsSent(DashboardData dashboardData,
                         DatabaseService databaseService,
                         DatabaseConfig databaseConfig,
                         Clock clock) {
        super(dashboardData, "Emails Sent", "Type", "Description", "Count");
        this.databaseService = databaseService;
        this.databaseConfig = databaseConfig;
        this.clock = clock;
    }

    public void addRow(String type, String description, String count) {
        addEntry(type, description, count);
    }

    public void addRow(EmailsSentDB emailsSentDB) {
        addRow(emailsSentDB.getType(), emailsSentDB.getDescription(), String.valueOf(emailsSentDB.getCount()));
    }

    public Job.Result populate() {
        final String errorText = "ERROR";
        AtomicReference<Job.Result> result = new AtomicReference<>(null);

        try {
            databaseService.execute(databaseConfig, connection -> {
                List<EmailsSentDB> response = databaseService.executePreparedStatement(connection,
                    EmailsSentDB.class, EMAILS_SENT_SQL);

                if (response == null || response.isEmpty()) {
                    addRow(errorText, errorText, errorText);
                    result.set(Job.Result.failed("No response from database"));
                } else {
                    response.forEach(this::addRow);
                }
            });
        } catch (Exception e) {
            log.error("Unable to get Emails Sent", e);
            addRow(errorText, errorText, errorText);
            result.set(Job.Result.failed("Failed to get Emails Sent information", e));
        }

        populateTimestamp(dashboardData, "Emails Sent", LocalDateTime.now(clock));
        return result.get() == null ? Job.Result.passed() : result.get();
    }
}
