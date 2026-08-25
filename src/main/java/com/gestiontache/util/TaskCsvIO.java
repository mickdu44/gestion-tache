package com.gestiontache.util;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.databind.MappingIterator;
import com.gestiontache.model.Priority;
import com.gestiontache.model.Task;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Exports/imports tasks to/from a simple CSV format (titre, description,
 * date, priorite, termine) so they can be backed up or shared outside the
 * application. Import is forgiving: rows with a missing title or an
 * unparsable date are skipped rather than failing the whole file.
 */
public final class TaskCsvIO {

    private static final CsvMapper MAPPER = new CsvMapper();

    private static final CsvSchema WRITE_SCHEMA = CsvSchema.builder()
            .addColumn("titre")
            .addColumn("description")
            .addColumn("date")
            .addColumn("priorite")
            .addColumn("termine")
            .setUseHeader(true)
            .build();

    private TaskCsvIO() {
    }

    public static void export(List<Task> tasks, Writer writer) throws IOException {
        List<Map<String, String>> rows = new ArrayList<>();
        for (Task task : tasks) {
            Map<String, String> row = new LinkedHashMap<>();
            row.put("titre", task.getTitle());
            row.put("description", task.getDescription() == null ? "" : task.getDescription());
            row.put("date", task.getDate().toString());
            row.put("priorite", task.getPriority().name());
            row.put("termine", String.valueOf(task.isCompleted()));
            rows.add(row);
        }
        MAPPER.writer(WRITE_SCHEMA).writeValue(writer, rows);
    }

    public static List<Task> importFrom(Reader reader) throws IOException {
        List<Task> tasks = new ArrayList<>();
        CsvSchema readSchema = CsvSchema.emptySchema().withHeader();
        MappingIterator<Map<String, String>> it =
                MAPPER.readerFor(Map.class).with(readSchema).readValues(reader);
        while (it.hasNext()) {
            Map<String, String> row = it.next();
            Task task = toTask(row);
            if (task != null) {
                tasks.add(task);
            }
        }
        return tasks;
    }

    private static Task toTask(Map<String, String> row) {
        String title = row.get("titre");
        if (title == null || title.isBlank()) {
            return null;
        }
        String dateRaw = row.get("date");
        LocalDate date;
        try {
            date = LocalDate.parse(dateRaw == null ? "" : dateRaw.trim());
        } catch (DateTimeParseException e) {
            return null;
        }

        String description = row.getOrDefault("description", "");
        Task task = new Task(title.trim(), description == null ? "" : description, date);

        String priorityRaw = row.get("priorite");
        if (priorityRaw != null && !priorityRaw.isBlank()) {
            try {
                task.setPriority(Priority.valueOf(priorityRaw.trim().toUpperCase()));
            } catch (IllegalArgumentException ignored) {
                // Keep the default priority (MOYENNE) for an unrecognised value.
            }
        }

        task.setCompleted(Boolean.parseBoolean(row.get("termine")));
        return task;
    }
}
