package com.gestiontache.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gestiontache.model.Task;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Persists tasks locally as a JSON file under the user's home directory.
 * No server, no database: everything a "client lourd" needs to keep working offline.
 */
public class TaskRepository {

    private static final Path DEFAULT_DATA_FILE =
            Paths.get(System.getProperty("user.home"), ".gestion-tache", "tasks.json");

    private final Path dataFile;
    private final ObjectMapper mapper;

    public TaskRepository() {
        this(DEFAULT_DATA_FILE);
    }

    public TaskRepository(Path dataFile) {
        this.dataFile = dataFile;
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
        this.mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    public List<Task> load() {
        try {
            if (!Files.exists(dataFile)) {
                return new ArrayList<>();
            }
            Task[] tasks = mapper.readValue(dataFile.toFile(), Task[].class);
            return new ArrayList<>(Arrays.asList(tasks));
        } catch (IOException e) {
            throw new RuntimeException("Impossible de charger les taches depuis " + dataFile, e);
        }
    }

    public void save(List<Task> tasks) {
        try {
            Path parent = dataFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            mapper.writeValue(dataFile.toFile(), tasks);
        } catch (IOException e) {
            throw new RuntimeException("Impossible d'enregistrer les taches dans " + dataFile, e);
        }
    }
}
