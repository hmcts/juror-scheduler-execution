package uk.gov.hmcts.juror.job.execution.util;

import uk.gov.hmcts.juror.standard.service.exceptions.InternalServerException;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileSearch {
    final Supplier<Stream<Path>> streamSupplier;
    final List<Predicate<Path>> pathPredicates;
    final List<Predicate<File>> filePredicates;


    public static FileSearch directory(File directory, boolean recursive) {
        return new FileSearch(directory, recursive);
    }

    private FileSearch(File directory, boolean recursive) {
        this.streamSupplier = () -> {
            try {
                return (recursive ? Files.walk(directory.toPath()) : Files.list(directory.toPath()));
            } catch (Exception e) {
                throw new InternalServerException("Failed to create file stream", e);
            }
        };
        this.pathPredicates = new ArrayList<>();
        this.filePredicates = new ArrayList<>();
        this.filePredicates.add(File::isFile);
    }

    public Set<File> search() {
        return this.streamSupplier.get()
            .filter(reduce(pathPredicates))
            .map(Path::toFile)
            .filter(reduce(filePredicates))
            .collect(Collectors.toUnmodifiableSet());
    }

    private <T> Predicate<T> reduce(List<Predicate<T>> predicates) {
        return predicates.stream().reduce(x -> true, Predicate::and);
    }

    public FileSearch setFileNameRegexFilter(String fileNameRegex) {
        if (fileNameRegex != null) {
            this.pathPredicates.add(path -> path.getFileName().toString().matches(fileNameRegex));
        }
        return this;
    }

    public FileSearch setMaxAge(long maxAge) {
        this.filePredicates.add(file -> file.lastModified() < maxAge);
        return this;
    }

    public FileSearch setOwner(final String owner) {
        this.pathPredicates.add(path -> {
            try {
                return Files.getOwner(path).getName().equals(owner);
            } catch (Exception e) {
                throw new InternalServerException("Failed to find owner of file: " + path);
            }
        });
        return this;
    }
}
