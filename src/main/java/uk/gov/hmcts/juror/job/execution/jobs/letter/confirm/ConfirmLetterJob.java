package uk.gov.hmcts.juror.job.execution.jobs.letter.confirm;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.juror.job.execution.jobs.letter.LetterJob;
import uk.gov.hmcts.juror.job.execution.service.contracts.DatabaseService;

@Component
public class ConfirmLetterJob extends LetterJob {

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

    @Autowired
    public ConfirmLetterJob(DatabaseService databaseService, ConfirmLetterConfig config) {
        super(databaseService, config.getDatabase(), "auto_generate.confirmation_letter", GENERATED_LETTER_COUNT_SQL);
    }
}
