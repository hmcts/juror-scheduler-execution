package uk.gov.hmcts.juror.job.execution.jobs.letter.withdraw;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.juror.job.execution.jobs.letter.LetterJob;
import uk.gov.hmcts.juror.job.execution.service.contracts.DatabaseService;

@Component
public class WithdrawLetterJob extends LetterJob {

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

    @Autowired
    public WithdrawLetterJob(DatabaseService databaseService, WithdrawLetterConfig config) {
        super(databaseService, config.getDatabase(), "auto_generate.withdrawal_letter", GENERATED_LETTER_COUNT_SQL);
    }

}
