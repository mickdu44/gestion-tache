package com.gestiontache.repository;

import com.gestiontache.model.Task;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskRepositoryTest {

    @TempDir
    Path tempDir;

    @Test
    void savesOneFilePerDayNamedByIsoDate() {
        TaskRepository repository = new TaskRepository(tempDir.resolve("days"));
        LocalDate day = LocalDate.of(2026, 3, 10);

        repository.saveDay(day, List.of(new Task("Tache", "", day)));

        assertTrue(Files.exists(tempDir.resolve("days").resolve("2026-03-10.json")));
    }

    @Test
    void loadAllReadsTasksFromEveryDayFile() {
        TaskRepository repository = new TaskRepository(tempDir.resolve("days"));
        LocalDate day1 = LocalDate.of(2026, 3, 10);
        LocalDate day2 = LocalDate.of(2026, 3, 11);
        repository.saveDay(day1, List.of(new Task("Tache 1", "", day1)));
        repository.saveDay(day2, List.of(new Task("Tache 2", "", day2), new Task("Tache 3", "", day2)));

        assertEquals(3, repository.loadAll().size());
    }

    @Test
    void savingAnEmptyListDeletesTheDayFile() {
        TaskRepository repository = new TaskRepository(tempDir.resolve("days"));
        LocalDate day = LocalDate.of(2026, 3, 10);
        repository.saveDay(day, List.of(new Task("Tache", "", day)));
        Path file = tempDir.resolve("days").resolve("2026-03-10.json");
        assertTrue(Files.exists(file));

        repository.saveDay(day, List.of());

        assertFalse(Files.exists(file));
    }

    @Test
    void loadAllOnMissingDirectoryReturnsEmptyList() {
        TaskRepository repository = new TaskRepository(tempDir.resolve("does-not-exist"));

        assertTrue(repository.loadAll().isEmpty());
    }

    @Test
    void otherDaysAreUntouchedWhenSavingOneDay() {
        TaskRepository repository = new TaskRepository(tempDir.resolve("days"));
        LocalDate day1 = LocalDate.of(2026, 3, 10);
        LocalDate day2 = LocalDate.of(2026, 3, 11);
        repository.saveDay(day1, List.of(new Task("Tache jour 1", "", day1)));
        repository.saveDay(day2, List.of(new Task("Tache jour 2", "", day2)));

        repository.saveDay(day1, List.of(new Task("Tache jour 1 modifiee", "", day1)));

        List<Task> all = repository.loadAll();
        assertEquals(2, all.size());
        assertTrue(all.stream().anyMatch(t -> t.getTitle().equals("Tache jour 2")));
    }
}
