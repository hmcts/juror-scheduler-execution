package uk.gov.hmcts.juror.job.execution.database;

import java.lang.annotation.*;
import java.util.function.Consumer;

@Documented
@Target( { ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface DatabaseColumn {
    String name();
    String setter();

    boolean isClob() default false;
}
