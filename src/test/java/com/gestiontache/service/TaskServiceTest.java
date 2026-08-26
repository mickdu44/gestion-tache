package com.gestiontache.service;

import com.gestiontache.model.Priority;
import com.gestiontache.model.Task;
import com.gestiontache.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskServiceTest {

    @TempDir
    Path tempDir;

    private TaskService service;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        TaskRepository repository = new TaskRepository(tempDir.resolve("days"));
        service = new TaskService(repository);
        today = LocalDate.now();
    }

    @Test
    void reportsUnfinishedTasksToNextDayOnly() {
        Task done = new Task("Livrer le rapport", "envoyer par mail", today);
        done.setCompleted(true);
        Task pending = new Task("Relire le contrat", "verifier les clauses", today);
        service.addTask(done);
        service.addTask(pending);

        int moved = service.reportUnfinishedToNextDay(today);

        assertEquals(1, moved);
        assertTrue(service.getTasksForDate(today).stream().anyMatch(t -> t.getId().equals(done.getId())));
        assertTrue(service.getTasksForDate(today.plusDays(1)).stream()
                .anyMatch(t -> t.getId().equals(pending.getId())));
    }

    @Test
    void reportsOverdueTasksToToday() {
        Task overdue = new Task("Preparer la reunion", "ordre du jour", today.minusDays(3));
        service.addTask(overdue);

        int moved = service.reportOverdueToToday(today);

        assertEquals(1, moved);
        assertTrue(service.getTasksForDate(today).stream().anyMatch(t -> t.getId().equals(overdue.getId())));
        assertTrue(service.getOverdueUnfinishedTasks(today).isEmpty());
    }

    @Test
    void searchMatchesTitleOrDescriptionAcrossAllDays() {
        service.addTask(new Task("Facturation client Dupont", "envoyer la facture", today));
        service.addTask(new Task("Reunion equipe", "parler du client Dupont", today.minusDays(1)));
        service.addTask(new Task("Achat fournitures", "papeterie", today));

        List<Task> results = service.search("dupont");

        assertEquals(2, results.size());
    }

    @Test
    void reorderTasksForDatePersistsManualOrder() {
        Task first = new Task("Preparer le café", "", today);
        Task second = new Task("Repondre aux mails", "", today);
        Task third = new Task("Rediger le compte-rendu", "", today);
        service.addTask(first);
        service.addTask(second);
        service.addTask(third);

        // Simulates a drag-and-drop: move "third" to the front.
        List<Task> newOrder = new ArrayList<>(List.of(third, first, second));
        service.reorderTasksForDate(today, newOrder);

        List<Task> result = service.getTasksForDate(today);
        assertEquals(List.of(third.getId(), first.getId(), second.getId()),
                result.stream().map(Task::getId).toList());
    }

    @Test
    void newTaskIsAppendedAfterExistingOnesOfTheSameDay() {
        Task first = new Task("Tache A", "", today);
        Task second = new Task("Tache B", "", today);
        service.addTask(first);
        service.addTask(second);

        assertTrue(second.getOrder() > first.getOrder());
        assertEquals(List.of(first.getId(), second.getId()),
                service.getTasksForDate(today).stream().map(Task::getId).toList());
    }

    @Test
    void defaultPriorityIsMoyenneAndSurvivesPersistence() {
        Task task = new Task("Tache par defaut", "", today);
        assertEquals(Priority.MOYENNE, task.getPriority());

        Task urgent = new Task("Tache urgente", "", today);
        urgent.setPriority(Priority.HAUTE);
        service.addTask(task);
        service.addTask(urgent);

        TaskService reloaded = new TaskService(new TaskRepository(tempDir.resolve("days")));
        List<Task> tasksForToday = reloaded.getTasksForDate(today);
        assertEquals(Priority.MOYENNE,
                tasksForToday.stream().filter(t -> t.getId().equals(task.getId())).findFirst().orElseThrow().getPriority());
        assertEquals(Priority.HAUTE,
                tasksForToday.stream().filter(t -> t.getId().equals(urgent.getId())).findFirst().orElseThrow().getPriority());
    }

    @Test
    void persistsAcrossServiceInstances() {
        TaskRepository repository = new TaskRepository(tempDir.resolve("persisted-days"));
        TaskService first = new TaskService(repository);
        first.addTask(new Task("Sauvegarder les donnees", "verification locale", today));

        TaskService second = new TaskService(new TaskRepository(tempDir.resolve("persisted-days")));

        assertEquals(1, second.getTasksForDate(today).size());
    }

    @Test
    void editingATaskToAnotherDateMovesItBetweenDayFiles() {
        Task task = new Task("Reunion", "", today);
        service.addTask(task);

        LocalDate previousDate = task.getDate();
        task.setDate(today.plusDays(2));
        service.updateTask(task, previousDate);

        assertTrue(service.getTasksForDate(today).isEmpty());
        assertEquals(1, service.getTasksForDate(today.plusDays(2)).size());

        // Reloading from disk proves the old day file no longer holds the task
        // and the new day file does: this is a cross-file move, not just an
        // in-memory change.
        TaskService reloaded = new TaskService(new TaskRepository(tempDir.resolve("days")));
        assertTrue(reloaded.getTasksForDate(today).isEmpty());
        assertEquals(1, reloaded.getTasksForDate(today.plusDays(2)).size());
    }

    @Test
    void searchStillSpansEveryDayFileAfterReload() {
        service.addTask(new Task("Facturation Dupont", "", today));
        service.addTask(new Task("Reunion", "parler du client Dupont", today.minusDays(10)));
        service.addTask(new Task("Achat fournitures", "", today.plusDays(5)));

        TaskService reloaded = new TaskService(new TaskRepository(tempDir.resolve("days")));

        assertEquals(2, reloaded.search("dupont").size());
    }
}
