package uk.gov.hmcts.juror.job.execution.jobs.dashboard.ams.data;

import uk.gov.hmcts.juror.job.execution.config.DatabaseConfig;
import uk.gov.hmcts.juror.job.execution.service.contracts.DatabaseService;

import java.time.Clock;

public class BureauLettersToBePrinted extends CommunicationCounts<BureauLettersToBePrintedDB> {
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
            when tfa.form_type ='5229C' then 'POSTPONE WELSH'
            when tfa.form_type ='6220' then 'DBD SUMMONS ENGLISH'
            when tfa.form_type ='6220C' then 'DBD SUMMONS WELSH'
            when tfa.form_type ='6228' then 'DBD REMINDER ENGLISH'
            when tfa.form_type ='6228C' then 'DBD REMINDER WELSH'
            when tfa.form_type ='6221' then 'DBD RESPONSE ENGLISH'
            when tfa.form_type ='6221C' then 'DBD RESPONSE WELSH' else 'UNKNOWN' end) description,
            coalesce(sum(form_counts.number_of_items),0)::bigint "count"
            from juror_mod.t_form_attr tfa left outer join (
               select bpd.form_type,
                    count(bpd.juror_no) as number_of_items
               from juror_mod.bulk_print_data bpd
               where bpd.extracted_date = current_date - 1
                   and bpd.communication_channel = 'LETTER'
             group by bpd.form_type) form_counts
            on tfa.form_type = form_counts.form_type
            group by tfa.form_type, tfa.dir_name
            order by 1;
        """;

    protected BureauLettersToBePrinted(DashboardData dashboardData,
                                       DatabaseService databaseService,
                                       DatabaseConfig databaseConfig,
                                       Clock clock) {
        super(dashboardData, "Bureau Letters To Be Printed", databaseService, databaseConfig, clock,
            BureauLettersToBePrintedDB.class, BUREAU_LETTERS_TO_BE_PRINTED_SQL, "Unexpected exception");
    }

    @Override
    public void addRow(BureauLettersToBePrintedDB bureauLettersToBePrintedDB) {
        this.addRow(bureauLettersToBePrintedDB.getType(),
            bureauLettersToBePrintedDB.getDescription(),
            String.valueOf(bureauLettersToBePrintedDB.getCount()));
    }
}
