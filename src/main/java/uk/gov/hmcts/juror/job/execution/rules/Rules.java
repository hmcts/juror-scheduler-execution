package uk.gov.hmcts.juror.job.execution.rules;

import java.io.File;
import java.util.Objects;
import java.util.function.Predicate;

public final class Rules {
    private Rules() {

    }

    static <T> Predicate<T> notNull() {
        return Objects::nonNull;
    }

    static <T> Predicate<T> notNull(Class<T> ignored) {
        return notNull();
    }

    static Predicate<File> isDirectory() {
        return File::isDirectory;
    }

    static Predicate<File> fileExists() {
        return File::exists;
    }

    public static Rule requireDirectory(final File directory) {
        return new RequireDirectoryRule(directory);
    }

    public record RequireDirectoryRule(File directory) implements Rule {

        @Override
        public boolean execute() {
            return notNull(File.class).and(isDirectory()).and(fileExists()).test(directory);
        }

        @Override
        public String getMessage() {
            if (execute()) {
                return null;
            }
            return directory == null
                ? "Directory must not be null" :
                directory.getAbsolutePath() + " must exist and be a directory";
        }
    }
}
