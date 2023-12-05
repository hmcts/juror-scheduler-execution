package uk.gov.hmcts.juror.job.execution.util;


import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.juror.standard.service.exceptions.InternalServerException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FileUtils {

    public static File createFile(String path) {
        try {
            File file = new File(path);
            if (!file.exists()) {
                file.createNewFile();
            }
            return file;
        } catch (Exception exception) {
            throw new InternalServerException("Failed to create new file on path: " + path, exception);
        }
    }

    public static void deleteFiles(Set<File> files) {
        files.parallelStream().forEach(FileUtils::deleteFile);
    }


    public static void deleteFile(File file) {
        try {
            Files.delete(file.toPath());
        } catch (Exception e) {
            throw new InternalServerException("Failed to delete file: " + file.getAbsolutePath());
        }
    }

    public static void move(File source, File destination, boolean failIfSourceNotFound) {

        if (!doesFileExist(source)) {
            if (failIfSourceNotFound) {
                throw new InternalServerException("File does not exist: " + source.getAbsolutePath());
            }
            return;
        }
        try {
            Files.move(source.toPath(), destination.toPath());
        } catch (Exception e) {
            throw new InternalServerException(
                "Failed to move file from: " + source.getAbsolutePath() + " to " + destination.getAbsolutePath());
        }
    }

    public static boolean doesFileExist(File checksFile) {
        return checksFile != null && checksFile.exists();
    }

    public static void writeToFile(File file, String data) throws IOException {
        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            final byte[] contentInBytes = data.getBytes();
            outputStream.write(contentInBytes);
            outputStream.flush();
        }
    }


    public static List<String> getLines(File file, String mustMatchRegex, String mustNotMatchRegex) {
        List<String> matchingLines = new ArrayList<>();
        try (Stream<String> lines = Files.lines(file.toPath())) {
            lines.forEach(line -> {
                if ((mustMatchRegex == null || line.matches(mustMatchRegex))
                    && (mustNotMatchRegex == null || !line.matches(mustNotMatchRegex))) {
                        matchingLines.add(line);
                }
            });
        } catch (IOException e) {
            throw new InternalServerException("Failed to read lines from file: " + file.getAbsolutePath(), e);
        }
        return matchingLines;
    }
}
