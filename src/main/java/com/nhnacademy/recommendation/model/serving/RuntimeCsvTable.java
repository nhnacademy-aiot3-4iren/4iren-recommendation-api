package com.nhnacademy.recommendation.model.serving;

import com.nhnacademy.recommendation.exception.BundleValidationException;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class RuntimeCsvTable {

    private static final char UTF_8_BOM = '\uFEFF';

    private final List<String> headers;
    private final List<List<String>> rows;

    private RuntimeCsvTable(List<String> headers, List<List<String>> rows) {
        this.headers = List.copyOf(headers);
        this.rows = List.copyOf(rows);
    }

    public static RuntimeCsvTable load(Path path) {
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new BundleValidationException("runtime CSV가 비어 있습니다: " + path.getFileName());
            }
            if (!headerLine.isEmpty() && headerLine.charAt(0) == UTF_8_BOM) {
                headerLine = headerLine.substring(1);
            }
            List<String> headers = parseLine(headerLine, path, 1);
            validateHeaders(headers, path);

            List<List<String>> rows = new ArrayList<>();
            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isEmpty()) {
                    continue;
                }
                List<String> row = parseLine(line, path, lineNumber);
                if (row.size() != headers.size()) {
                    throw new BundleValidationException(
                            "runtime CSV 컬럼 수가 header와 다릅니다: " + path.getFileName()
                                    + " line=" + lineNumber + " expected=" + headers.size() + " actual=" + row.size()
                    );
                }
                rows.add(List.copyOf(row));
            }
            return new RuntimeCsvTable(headers, rows);
        } catch (IOException e) {
            throw new BundleValidationException("runtime CSV를 읽을 수 없습니다: " + path, e);
        }
    }

    public List<String> headers() {
        return headers;
    }

    public List<List<String>> rows() {
        return rows;
    }

    public int columnIndex(String name) {
        int index = headers.indexOf(name);
        if (index < 0) {
            throw new BundleValidationException("runtime CSV 필수 컬럼이 없습니다: " + name);
        }
        return index;
    }

    private static void validateHeaders(List<String> headers, Path path) {
        if (headers.isEmpty()) {
            throw new BundleValidationException("runtime CSV header가 비어 있습니다: " + path.getFileName());
        }
        Set<String> unique = new HashSet<>();
        for (String header : headers) {
            if (header == null || header.isBlank()) {
                throw new BundleValidationException("runtime CSV에 빈 header가 있습니다: " + path.getFileName());
            }
            if (!unique.add(header)) {
                throw new BundleValidationException(
                        "runtime CSV에 중복 header가 있습니다: " + path.getFileName() + " header=" + header
                );
            }
        }
    }

    private static List<String> parseLine(String line, Path path, int lineNumber) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;

        for (int index = 0; index < line.length(); index++) {
            char character = line.charAt(index);
            if (character == '"') {
                if (quoted && index + 1 < line.length() && line.charAt(index + 1) == '"') {
                    current.append('"');
                    index++;
                } else {
                    quoted = !quoted;
                }
            } else if (character == ',' && !quoted) {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(character);
            }
        }
        if (quoted) {
            throw new BundleValidationException(
                    "runtime CSV 따옴표가 닫히지 않았습니다: " + path.getFileName() + " line=" + lineNumber
            );
        }
        values.add(current.toString());
        return values;
    }
}
