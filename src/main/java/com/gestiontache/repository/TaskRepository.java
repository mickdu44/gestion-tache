package com.gestiontache.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gestiontache.model.Task;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Persists tasks locally, one JSON file per day (named {@code yyyy-MM-dd.json})
 * under a data directory in the user's home. No server, no database:
 * everything a "client lourd" needs to keep working offline.
 */
public class TaskRepository {

    private static final Path DEFAULT_DATA_DIR =
            Paths.get(System.getProperty("user.home"), ".gestion-tache", "days");
    private static final Path LEGACY_SINGLE_FILE =
            Paths.get(System.getProperty("user.home"), ".gestion-tache", "tasks.json");
    private static final Path DEFAULT_ARCHIVE_DIR =
            Paths.get(System.getProperty("user.home"), ".gestion-tache", "archive-days");

    private final Path dataDir;
    private final ObjectMapper mapper;

    public TaskRepository() {
        this(DEFAULT_DATA_DIR);
        migrateLegacySingleFileIfNeeded();
    }

    /**
     * Repository for the archive, stored the same way as the main data (one
     * JSON file per original completion day) but under its own directory.
     */
    public static TaskRepository defaultArchive() {
        return new TaskRepository(DEFAULT_ARCHIVE_DIR);
    }

    public TaskRepository(Path dataDir) {
        this.dataDir = dataDir;
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
        this.mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    /** Loads every task from every day file in the data directory. */
    public List<Task> loadAll() {
        if (!Files.isDirectory(dataDir)) {
            return new ArrayList<>();
        }
        try (Stream<Path> files = Files.list(dataDir)) {
            List<Path> dayFiles = files.filter(p -> p.toString().endsWith(".json"))
                    .sorted()
                    .toList();
            List<Task> all = new ArrayList<>();
            for (Path file : dayFiles) {
                all.addAll(readFile(file));
            }
            return all;
        } catch (IOException e) {
            throw new RuntimeException("Impossible de charger les taches depuis " + dataDir, e);
        }
    }

    /**
     * Writes the day file for {@code date} to contain exactly
     * {@code tasksForDay}, or deletes it when that list is empty (a day with
     * no tasks left has no reason to keep a file around).
     */
    public void saveDay(LocalDate date, List<Task> tasksForDay) {
        Path file = fileFor(date);
        try {
            if (tasksForDay.isEmpty()) {
                Files.deleteIfExists(file);
                return;
            }
            Files.createDirectories(dataDir);
            mapper.writeValue(file.toFile(), tasksForDay);
        } catch (IOException e) {
            throw new RuntimeException("Impossible d'enregistrer les taches dans " + file, e);
        }
    }

    private Path fileFor(LocalDate date) {
        return dataDir.resolve(date + ".json");
    }

    private List<Task> readFile(Path file) {
        try {
            Task[] tasks = mapper.readValue(file.toFile(), Task[].class);
            return new ArrayList<>(Arrays.asList(tasks));
        } catch (IOException e) {
            throw new RuntimeException("Impossible de charger les taches depuis " + file, e);
        }
    }

    /**
     * One-time upgrade path: if an old single-file installation is found and
     * the new day-file directory is still empty, splits its content into
     * per-day files. The legacy file itself is left untouched as a backup.
     */
    private void migrateLegacySingleFileIfNeeded() {
        if (!Files.exists(LEGACY_SINGLE_FILE)) {
            return;
        }
        try {
            if (Files.isDirectory(dataDir)) {
                try (Stream<Path> existing = Files.list(dataDir)) {
                    if (existing.findAny().isPresent()) {
                        return;
                    }
                }
            }
            List<Task> legacyTasks = readFile(LEGACY_SINGLE_FILE);
            Map<LocalDate, List<Task>> byDate = legacyTasks.stream()
                    .collect(Collectors.groupingBy(Task::getDate));
            for (Map.Entry<LocalDate, List<Task>> entry : byDate.entrySet()) {
                saveDay(entry.getKey(), entry.getValue());
            }
        } catch (IOException e) {
            throw new RuntimeException(
                    "Echec de la migration de l'ancien fichier " + LEGACY_SINGLE_FILE + " vers le stockage journalier", e);
        }
    }
}
