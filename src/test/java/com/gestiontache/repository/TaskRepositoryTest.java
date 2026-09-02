package com.gestiontache.repository;

import com.gestiontache.model.SubTask;
import com.gestiontache.model.Task;
import com.gestiontache.model.TaskStatus;
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

    @Test
    void subtasksAndAttachmentsSurviveARoundTrip() {
        TaskRepository repository = new TaskRepository(tempDir.resolve("days"));
        LocalDate day = LocalDate.of(2026, 3, 10);
        Task task = new Task("Preparer le dossier", "", day);
        SubTask done = new SubTask("Recolter les pieces");
        done.setCompleted(true);
        task.setSubtasks(List.of(done, new SubTask("Envoyer au client")));
        task.setAttachments(List.of("/home/user/documents/dossier.pdf"));

        repository.saveDay(day, List.of(task));

        Task reloaded = repository.loadAll().get(0);
        assertEquals(2, reloaded.getSubtasks().size());
        assertTrue(reloaded.getSubtasks().stream()
                .anyMatch(s -> s.getTitle().equals("Recolter les pieces") && s.isCompleted()));
        assertTrue(reloaded.getSubtasks().stream()
                .anyMatch(s -> s.getTitle().equals("Envoyer au client") && !s.isCompleted()));
        assertEquals(List.of("/home/user/documents/dossier.pdf"), reloaded.getAttachments());
    }

    @Test
    void taskWithoutSubtasksOrAttachmentsLoadsWithEmptyLists() {
        TaskRepository repository = new TaskRepository(tempDir.resolve("days"));
        LocalDate day = LocalDate.of(2026, 3, 10);
        repository.saveDay(day, List.of(new Task("Tache simple", "", day)));

        Task reloaded = repository.loadAll().get(0);

        assertTrue(reloaded.getSubtasks().isEmpty());
        assertTrue(reloaded.getAttachments().isEmpty());
    }

    @Test
    void historySurvivesARoundTrip() {
        TaskRepository repository = new TaskRepository(tempDir.resolve("days"));
        LocalDate day = LocalDate.of(2026, 3, 10);
        Task task = new Task("Preparer le dossier", "", day);
        task.addHistoryEntry("Titre modifie : \"a\" -> \"b\"");

        repository.saveDay(day, List.of(task));

        Task reloaded = repository.loadAll().get(0);
        assertEquals(2, reloaded.getHistory().size());
        assertEquals("Tache creee", reloaded.getHistory().get(0).getMessage());
        assertEquals("Titre modifie : \"a\" -> \"b\"", reloaded.getHistory().get(1).getMessage());
    }

    /**
     * A day file written before the "history" field existed has no such
     * key at all. Loading it must not fail and must expose an empty history
     * rather than null.
     */
    @Test
    void legacyFileWithoutHistoryFieldLoadsWithEmptyHistory() throws Exception {
        Path daysDir = tempDir.resolve("days");
        Files.createDirectories(daysDir);
        String legacyJson = "[ {\n"
                + "  \"id\" : \"legacy-1\",\n"
                + "  \"title\" : \"Tache historique\",\n"
                + "  \"description\" : \"\",\n"
                + "  \"date\" : \"2026-03-10\",\n"
                + "  \"completed\" : false,\n"
                + "  \"createdAt\" : \"2026-03-10T09:00:00\",\n"
                + "  \"order\" : 0,\n"
                + "  \"priority\" : \"MOYENNE\"\n"
                + "} ]";
        Files.writeString(daysDir.resolve("2026-03-10.json"), legacyJson);

        TaskRepository repository = new TaskRepository(daysDir);
        List<Task> loaded = repository.loadAll();

        assertTrue(loaded.get(0).getHistory().isEmpty());
    }

    @Test
    void statusSurvivesARoundTrip() {
        TaskRepository repository = new TaskRepository(tempDir.resolve("days"));
        LocalDate day = LocalDate.of(2026, 3, 10);
        Task task = new Task("Preparer le dossier", "", day);
        task.setStatus(TaskStatus.EN_COURS);

        repository.saveDay(day, List.of(task));

        Task reloaded = repository.loadAll().get(0);
        assertEquals(TaskStatus.EN_COURS, reloaded.getStatus());
        assertFalse(reloaded.isCompleted());
    }

    /**
     * A day file written before the "status" field existed only has the
     * legacy "completed" boolean. Loading it must still derive the right
     * status instead of failing or silently defaulting everything to
     * A_FAIRE regardless of completion.
     */
    @Test
    void legacyFileWithOnlyCompletedFieldDerivesStatus() throws Exception {
        Path daysDir = tempDir.resolve("days");
        Files.createDirectories(daysDir);
        String legacyJson = "[ {\n"
                + "  \"id\" : \"legacy-1\",\n"
                + "  \"title\" : \"Tache terminee historique\",\n"
                + "  \"description\" : \"\",\n"
                + "  \"date\" : \"2026-03-10\",\n"
                + "  \"completed\" : true,\n"
                + "  \"createdAt\" : \"2026-03-10T09:00:00\",\n"
                + "  \"order\" : 0,\n"
                + "  \"priority\" : \"MOYENNE\"\n"
                + "}, {\n"
                + "  \"id\" : \"legacy-2\",\n"
                + "  \"title\" : \"Tache non terminee historique\",\n"
                + "  \"description\" : \"\",\n"
                + "  \"date\" : \"2026-03-10\",\n"
                + "  \"completed\" : false,\n"
                + "  \"createdAt\" : \"2026-03-10T09:05:00\",\n"
                + "  \"order\" : 1,\n"
                + "  \"priority\" : \"MOYENNE\"\n"
                + "} ]";
        Files.writeString(daysDir.resolve("2026-03-10.json"), legacyJson);

        TaskRepository repository = new TaskRepository(daysDir);
        List<Task> loaded = repository.loadAll();

        Task completedLegacy = loaded.stream().filter(t -> t.getId().equals("legacy-1")).findFirst().orElseThrow();
        Task pendingLegacy = loaded.stream().filter(t -> t.getId().equals("legacy-2")).findFirst().orElseThrow();
        assertEquals(TaskStatus.TERMINEE, completedLegacy.getStatus());
        assertTrue(completedLegacy.isCompleted());
        assertEquals(TaskStatus.A_FAIRE, pendingLegacy.getStatus());
        assertFalse(pendingLegacy.isCompleted());
    }
}
