package com.gestiontache.service;

import com.gestiontache.model.Priority;
import com.gestiontache.model.Task;
import com.gestiontache.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Automatic archiving only kicks in when a {@code TaskService} is built with
 * an archive repository; the single-argument constructor used throughout
 * {@link TaskServiceTest} deliberately leaves it disabled.
 */
class TaskArchivingTest {

    @TempDir
    Path tempDir;

    private TaskService newServiceWithArchive() {
        TaskRepository repository = new TaskRepository(tempDir.resolve("days"));
        TaskRepository archive = new TaskRepository(tempDir.resolve("archive-days"));
        return new TaskService(repository, archive);
    }

    @Test
    void completedTaskOlderThanThreeMonthsIsArchivedOnStartup() {
        LocalDate today = LocalDate.now();
        TaskRepository repository = new TaskRepository(tempDir.resolve("days"));
        TaskService seedingService = new TaskService(repository);
        Task old = new Task("Vieille tache terminee", "", today.minusMonths(4));
        old.setCompleted(true);
        seedingService.addTask(old);

        TaskRepository archive = new TaskRepository(tempDir.resolve("archive-days"));
        TaskService service = new TaskService(repository, archive);

        assertTrue(service.getTasksForDate(today.minusMonths(4)).isEmpty());
        assertEquals(1, service.getArchivedTasks().size());
        assertEquals("Vieille tache terminee", service.getArchivedTasks().get(0).getTitle());
    }

    @Test
    void recentCompletedTaskIsNotArchived() {
        TaskService service = newServiceWithArchive();
        LocalDate today = LocalDate.now();
        Task recent = new Task("Tache recente terminee", "", today.minusDays(5));
        recent.setCompleted(true);
        service.addTask(recent);

        // Re-create the service to re-run the startup sweep, as would happen on the next launch.
        TaskRepository repository = new TaskRepository(tempDir.resolve("days"));
        TaskRepository archive = new TaskRepository(tempDir.resolve("archive-days"));
        TaskService reloaded = new TaskService(repository, archive);

        assertTrue(reloaded.getArchivedTasks().isEmpty());
        assertEquals(1, reloaded.getTasksForDate(today.minusDays(5)).size());
    }

    @Test
    void oldUnfinishedTaskIsNotArchived() {
        TaskService service = newServiceWithArchive();
        LocalDate today = LocalDate.now();
        Task oldButUnfinished = new Task("Vieille tache non terminee", "", today.minusMonths(5));
        service.addTask(oldButUnfinished);

        TaskRepository repository = new TaskRepository(tempDir.resolve("days"));
        TaskRepository archive = new TaskRepository(tempDir.resolve("archive-days"));
        TaskService reloaded = new TaskService(repository, archive);

        assertTrue(reloaded.getArchivedTasks().isEmpty());
        assertEquals(1, reloaded.getTasksForDate(today.minusMonths(5)).size());
    }

    @Test
    void archivingIsDisabledWithoutAnArchiveRepository() {
        TaskRepository repository = new TaskRepository(tempDir.resolve("days"));
        TaskService service = new TaskService(repository);
        LocalDate today = LocalDate.now();
        Task old = new Task("Tres vieille tache", "", today.minusYears(1));
        old.setCompleted(true);
        service.addTask(old);

        TaskService reloaded = new TaskService(new TaskRepository(tempDir.resolve("days")));

        assertTrue(reloaded.getArchivedTasks().isEmpty());
        assertEquals(1, reloaded.getTasksForDate(today.minusYears(1)).size());
    }

    @Test
    void restoringAnArchivedTaskMovesItBackToTheActiveList() {
        LocalDate today = LocalDate.now();
        TaskRepository repository = new TaskRepository(tempDir.resolve("days"));
        TaskService seedingService = new TaskService(repository);
        Task old = new Task("A restaurer", "", today.minusMonths(4));
        old.setCompleted(true);
        old.setPriority(Priority.HAUTE);
        seedingService.addTask(old);

        TaskRepository archive = new TaskRepository(tempDir.resolve("archive-days"));
        TaskService service = new TaskService(repository, archive);
        Task archived = service.getArchivedTasks().get(0);

        service.restoreFromArchive(archived);

        assertTrue(service.getArchivedTasks().isEmpty());
        List<Task> backInActiveList = service.getTasksForDate(today.minusMonths(4));
        assertEquals(1, backInActiveList.size());
        assertTrue(backInActiveList.get(0).isCompleted());
        assertEquals(Priority.HAUTE, backInActiveList.get(0).getPriority());
    }

    @Test
    void deletingFromArchiveIsPermanentAndDoesNotAffectTheActiveList() {
        LocalDate today = LocalDate.now();
        TaskRepository repository = new TaskRepository(tempDir.resolve("days"));
        TaskService seedingService = new TaskService(repository);
        Task old = new Task("A supprimer", "", today.minusMonths(6));
        old.setCompleted(true);
        seedingService.addTask(old);

        TaskRepository archive = new TaskRepository(tempDir.resolve("archive-days"));
        TaskService service = new TaskService(repository, archive);
        Task archived = service.getArchivedTasks().get(0);

        service.deleteFromArchive(archived);

        assertTrue(service.getArchivedTasks().isEmpty());
        assertFalse(service.getTasksForDate(today.minusMonths(6)).stream()
                .anyMatch(t -> t.getId().equals(archived.getId())));
    }
}
