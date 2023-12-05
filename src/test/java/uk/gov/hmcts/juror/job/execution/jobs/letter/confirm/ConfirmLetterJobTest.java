package uk.gov.hmcts.juror.job.execution.jobs.letter.confirm;

import uk.gov.hmcts.juror.job.execution.jobs.letter.AbstractLetterJobTest;
import uk.gov.hmcts.juror.job.execution.service.contracts.DatabaseService;

public class ConfirmLetterJobTest extends AbstractLetterJobTest<ConfirmLetterJob, ConfirmLetterConfig> {
    private static final String GENERATED_LETTER_COUNT_SQL = """
        select count(1)  count
        from print_files f, part_hist h
        where creation_date > trunc(sysdate-1)
        and creation_date < trunc(sysdate)
        and form_type in ('5224A','5224AC')
        and h.owner = '400'
        and h.part_no = decode(form_type,'5224AC',substr(detail_rec,656,9),substr(detail_rec,675,9))
        and history_code = 'RRES'
        and date_part > trunc(sysdate - 1)
        and h.user_id = 'SYSTEM'
        and h.other_information = 'Confirmation Letter Auto'""";


    @Override
    protected ConfirmLetterJob createJob(DatabaseService databaseService, ConfirmLetterConfig config) {
        return new ConfirmLetterJob(databaseService, config);
    }

    @Override
    protected ConfirmLetterConfig createConfig() {
        return new ConfirmLetterConfig();
    }

    @Override
    protected String getCountSql() {
        return GENERATED_LETTER_COUNT_SQL;
    }

    @Override
    protected String getProcedureName() {
        return "auto_generate.confirmation_letter";
    }
}
