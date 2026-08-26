package com.gestiontache.service;

import com.gestiontache.model.Priority;
import com.gestiontache.model.Recurrence;
import com.gestiontache.model.Task;
import com.gestiontache.model.TaskStatistics;
import com.gestiontache.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    void completingADailyRecurringTaskCreatesTomorrowsOccurrence() {
        Task task = new Task("Arroser les plantes", "", today);
        task.setRecurrence(Recurrence.QUOTIDIENNE);
        service.addTask(task);

        service.setCompleted(task, true);

        List<Task> tomorrow = service.getTasksForDate(today.plusDays(1));
        assertEquals(1, tomorrow.size());
        Task next = tomorrow.get(0);
        assertEquals("Arroser les plantes", next.getTitle());
        assertFalse(next.isCompleted());
        assertEquals(Recurrence.QUOTIDIENNE, next.getRecurrence());

        // Both the completed task's day file and the new occurrence's day
        // file must be written, not just the former.
        TaskService reloaded = new TaskService(new TaskRepository(tempDir.resolve("days")));
        assertEquals(1, reloaded.getTasksForDate(today.plusDays(1)).size());
    }

    @Test
    void completingAWeeklyRecurringTaskSchedulesOneWeekLater() {
        Task task = new Task("Faire le point hebdo", "", today);
        task.setRecurrence(Recurrence.HEBDOMADAIRE);
        service.addTask(task);

        service.setCompleted(task, true);

        assertEquals(1, service.getTasksForDate(today.plusWeeks(1)).size());
    }

    @Test
    void completingABusinessDayRecurringTaskSkipsWeekends() {
        LocalDate friday = today.with(DayOfWeek.FRIDAY);
        Task task = new Task("Backup quotidien", "", friday);
        task.setRecurrence(Recurrence.JOURS_OUVRES);
        service.addTask(task);

        service.setCompleted(task, true);

        LocalDate monday = friday.plusDays(3);
        assertEquals(1, service.getTasksForDate(monday).size());
    }

    @Test
    void toggleWithoutRecurrenceDoesNotCreateAnyOccurrence() {
        Task task = new Task("Tache simple", "", today);
        service.addTask(task);

        service.setCompleted(task, true);

        assertEquals(1, service.getTasksForDate(today).size());
    }

    @Test
    void reCompletingAnAlreadyCompletedTaskDoesNotDuplicateOccurrences() {
        Task task = new Task("Pointage quotidien", "", today);
        task.setRecurrence(Recurrence.QUOTIDIENNE);
        service.addTask(task);

        service.setCompleted(task, true);
        service.setCompleted(task, true);

        assertEquals(1, service.getTasksForDate(today.plusDays(1)).size());
    }

    @Test
    void computeStatisticsCountsTotalsAndCompletionRate() {
        Task done = new Task("Tache faite", "", today);
        done.setCompleted(true);
        Task pending = new Task("Tache en cours", "", today);
        service.addTask(done);
        service.addTask(pending);

        TaskStatistics stats = service.computeStatistics(today, 7);

        assertEquals(2, stats.totalTasks());
        assertEquals(1, stats.completedTasks());
        assertEquals(0.5, stats.completionRate());
    }

    @Test
    void computeStatisticsReturnsZeroRateWhenNoTasks() {
        TaskStatistics stats = service.computeStatistics(today, 7);

        assertEquals(0, stats.totalTasks());
        assertEquals(0.0, stats.completionRate());
    }

    @Test
    void computeStatisticsBucketsCompletedTasksByTheirOwnDateOldestFirst() {
        Task threeDaysAgo = new Task("Tache 1", "", today.minusDays(3));
        threeDaysAgo.setCompleted(true);
        Task yesterday = new Task("Tache 2", "", today.minusDays(1));
        yesterday.setCompleted(true);
        Task notCompletedToday = new Task("Tache 3", "", today);
        service.addTask(threeDaysAgo);
        service.addTask(yesterday);
        service.addTask(notCompletedToday);

        TaskStatistics stats = service.computeStatistics(today, 7);
        List<LocalDate> orderedDays = new ArrayList<>(stats.completedPerDay().keySet());

        assertEquals(today.minusDays(6), orderedDays.get(0));
        assertEquals(today, orderedDays.get(orderedDays.size() - 1));
        assertEquals(1L, stats.completedPerDay().get(today.minusDays(3)));
        assertEquals(1L, stats.completedPerDay().get(today.minusDays(1)));
        assertEquals(0L, stats.completedPerDay().get(today));
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
