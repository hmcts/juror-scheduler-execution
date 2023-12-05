package uk.gov.hmcts.juror.job.execution.jobs.letter.withdraw;

import uk.gov.hmcts.juror.job.execution.jobs.letter.AbstractLetterJobTest;
import uk.gov.hmcts.juror.job.execution.service.contracts.DatabaseService;

public class WithdrawLetterJobTest extends AbstractLetterJobTest<WithdrawLetterJob,WithdrawLetterConfig> {
    private static final String GENERATED_LETTER_COUNT_SQL = """
            select count(1) count
            from print_files f, part_hist h
            where creation_date > trunc(sysdate-1)
            and creation_date < trunc(sysdate)
            and form_type in ('5224','5224C')
            and h.owner = '400'
            and h.part_no = decode(form_type,'5224C',substr(detail_rec,613,9),substr(detail_rec,632,9))
            and history_code = 'RDIS'
            and date_part > trunc(sysdate - 1)
            and h.user_id = 'SYSTEM'
            and h.other_information = 'Withdrawal Letter Auto'
        """;
    @Override
    protected WithdrawLetterJob createJob(DatabaseService databaseService, WithdrawLetterConfig config) {
        return new WithdrawLetterJob(databaseService, config);
    }

    @Override
    protected WithdrawLetterConfig createConfig() {
        return new WithdrawLetterConfig();
    }

    @Override
    protected String getCountSql() {
        return GENERATED_LETTER_COUNT_SQL;
    }

    @Override
    protected String getProcedureName() {
        return "auto_generate.withdrawal_letter";
    }
}
