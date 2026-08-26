package com.gestiontache.service;

import com.gestiontache.model.Task;
import com.gestiontache.repository.TaskRepository;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * In-memory task list backed by {@link TaskRepository}. Every mutation is
 * persisted immediately so the local JSON file always reflects the current state.
 */
public class TaskService {

    /** Completed tasks older than this are swept into the archive on startup. */
    private static final Period ARCHIVE_AGE = Period.ofMonths(3);

    private final TaskRepository repository;
    private final TaskRepository archiveRepository;
    private final List<Task> tasks;
    private final List<Task> archivedTasks;

    public TaskService(TaskRepository repository) {
        this(repository, null);
    }

    /**
     * @param archiveRepository where completed tasks older than three months
     *                          are moved to on construction; pass {@code null}
     *                          to disable automatic archiving entirely.
     */
    public TaskService(TaskRepository repository, TaskRepository archiveRepository) {
        this.repository = repository;
        this.archiveRepository = archiveRepository;
        this.tasks = repository.load();
        this.archivedTasks = archiveRepository != null ? archiveRepository.load() : new ArrayList<>();
        archiveOldCompletedTasks(LocalDate.now());
    }

    public List<Task> getTasksForDate(LocalDate date) {
        return tasks.stream()
                .filter(t -> date.equals(t.getDate()))
                .sorted(Comparator.comparingInt(Task::getOrder).thenComparing(Task::getCreatedAt))
                .collect(Collectors.toList());
    }

    /** Searches every task, regardless of its date, by title or description. */
    public List<Task> search(String keyword) {
        return tasks.stream()
                .filter(t -> t.matches(keyword))
                .sorted(Comparator.comparing(Task::getDate).reversed())
                .collect(Collectors.toList());
    }

    public List<Task> getOverdueUnfinishedTasks(LocalDate today) {
        return tasks.stream()
                .filter(t -> !t.isCompleted() && t.getDate().isBefore(today))
                .collect(Collectors.toList());
    }

    public void addTask(Task task) {
        task.setOrder(nextOrderForDate(task.getDate()));
        tasks.add(task);
        persist();
    }

    /**
     * Applies a new manual order to the tasks of {@code date}, following the
     * order given by {@code orderedTasks} (typically the list view's items
     * after a drag-and-drop move).
     */
    public void reorderTasksForDate(LocalDate date, List<Task> orderedTasks) {
        int order = 0;
        for (Task t : orderedTasks) {
            if (date.equals(t.getDate())) {
                t.setOrder(order++);
            }
        }
        persist();
    }

    /** The task object is mutated in place by the caller; this simply persists it. */
    public void updateTask(Task task) {
        persist();
    }

    public void deleteTask(Task task) {
        tasks.remove(task);
        persist();
    }

    public void setCompleted(Task task, boolean completed) {
        task.setCompleted(completed);
        persist();
    }

    /**
     * Reports every unfinished task of {@code date} to the next day.
     * @return the number of tasks moved.
     */
    public int reportUnfinishedToNextDay(LocalDate date) {
        List<Task> unfinished = tasks.stream()
                .filter(t -> date.equals(t.getDate()) && !t.isCompleted())
                .collect(Collectors.toList());
        LocalDate next = date.plusDays(1);
        int order = nextOrderForDate(next);
        for (Task t : unfinished) {
            t.setDate(next);
            t.setOrder(order++);
        }
        if (!unfinished.isEmpty()) {
            persist();
        }
        return unfinished.size();
    }

    /**
     * Reports every unfinished task from a previous day to {@code today}.
     * @return the number of tasks moved.
     */
    public int reportOverdueToToday(LocalDate today) {
        List<Task> overdue = getOverdueUnfinishedTasks(today);
        int order = nextOrderForDate(today);
        for (Task t : overdue) {
            t.setDate(today);
            t.setOrder(order++);
        }
        if (!overdue.isEmpty()) {
            persist();
        }
        return overdue.size();
    }

    /** Tasks currently in the archive, most recently completed first. */
    public List<Task> getArchivedTasks() {
        return archivedTasks.stream()
                .sorted(Comparator.comparing(Task::getDate).reversed())
                .collect(Collectors.toList());
    }

    /** Moves an archived task back into the active list, keeping its completed state. */
    public void restoreFromArchive(Task task) {
        if (!archivedTasks.remove(task)) {
            return;
        }
        task.setOrder(nextOrderForDate(task.getDate()));
        tasks.add(task);
        persist();
        persistArchive();
    }

    /** Permanently removes a task from the archive (not recoverable). */
    public void deleteFromArchive(Task task) {
        if (archivedTasks.remove(task)) {
            persistArchive();
        }
    }

    /**
     * Sweeps completed tasks older than {@link #ARCHIVE_AGE} out of the
     * active list and into the archive. Runs automatically when the service
     * is constructed with an archive repository; a no-op otherwise.
     */
    private void archiveOldCompletedTasks(LocalDate today) {
        if (archiveRepository == null) {
            return;
        }
        LocalDate cutoff = today.minus(ARCHIVE_AGE);
        List<Task> toArchive = tasks.stream()
                .filter(t -> t.isCompleted() && t.getDate().isBefore(cutoff))
                .collect(Collectors.toList());
        if (toArchive.isEmpty()) {
            return;
        }
        tasks.removeAll(toArchive);
        archivedTasks.addAll(toArchive);
        persist();
        persistArchive();
    }

    private void persistArchive() {
        if (archiveRepository != null) {
            archiveRepository.save(archivedTasks);
        }
    }

    private int nextOrderForDate(LocalDate date) {
        return tasks.stream()
                .filter(t -> date.equals(t.getDate()))
                .mapToInt(Task::getOrder)
                .max()
                .orElse(-1) + 1;
    }

    private void persist() {
        repository.save(tasks);
    }
}
