package uk.gov.hmcts.juror.job.execution.jobs.dashboard.ams.data;

import uk.gov.hmcts.juror.job.execution.config.DatabaseConfig;
import uk.gov.hmcts.juror.job.execution.service.contracts.DatabaseService;

import java.time.Clock;

public class EmailsSent extends CommunicationCounts<EmailsSentDB> {
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

    protected EmailsSent(DashboardData dashboardData,
                         DatabaseService databaseService,
                         DatabaseConfig databaseConfig,
                         Clock clock) {
        super(dashboardData, "Emails Sent", databaseService, databaseConfig, clock,
            EmailsSentDB.class, EMAILS_SENT_SQL, "Failed to get Emails Sent information");
    }

    @Override
    public void addRow(EmailsSentDB emailsSentDB) {
        addRow(emailsSentDB.getType(), emailsSentDB.getDescription(), String.valueOf(emailsSentDB.getCount()));
    }
}
