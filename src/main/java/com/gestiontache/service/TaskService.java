package com.gestiontache.service;

import com.gestiontache.model.Recurrence;
import com.gestiontache.model.Task;
import com.gestiontache.repository.TaskRepository;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * In-memory task list backed by {@link TaskRepository}, which stores one
 * JSON file per day. Every mutation is persisted immediately, rewriting only
 * the day file(s) actually affected (never the whole dataset), so the local
 * files always reflect the current state.
 */
public class TaskService {

    private final TaskRepository repository;
    private final List<Task> tasks;

    public TaskService(TaskRepository repository) {
        this.repository = repository;
        this.tasks = repository.loadAll();
    }

    public List<Task> getTasksForDate(LocalDate date) {
        return tasks.stream()
                .filter(t -> date.equals(t.getDate()))
                .sorted(Comparator.comparingInt(Task::getOrder).thenComparing(Task::getCreatedAt))
                .collect(Collectors.toList());
    }

    /** Searches every task, across every day file, by title or description. */
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
        persistDate(task.getDate());
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
        persistDate(date);
    }

    /**
     * The task object is mutated in place by the caller (including its date,
     * for an edit that moves it to another day); this persists whichever day
     * file(s) are actually affected.
     */
    public void updateTask(Task task, LocalDate previousDate) {
        if (previousDate.equals(task.getDate())) {
            persistDate(task.getDate());
        } else {
            persistDates(Set.of(previousDate, task.getDate()));
        }
    }

    public void deleteTask(Task task) {
        LocalDate date = task.getDate();
        tasks.remove(task);
        persistDate(date);
    }

    /**
     * Marks a task done or not. When a task with a recurrence rule
     * transitions to completed, the next occurrence is created automatically
     * (not completed) on the corresponding future date.
     */
    public void setCompleted(Task task, boolean completed) {
        boolean wasCompleted = task.isCompleted();
        task.setCompleted(completed);
        if (completed && !wasCompleted && task.getRecurrence() != Recurrence.AUCUNE) {
            Task next = createNextOccurrence(task);
            persistDates(Set.of(task.getDate(), next.getDate()));
        } else {
            persistDate(task.getDate());
        }
    }

    private Task createNextOccurrence(Task task) {
        LocalDate nextDate = task.getRecurrence().nextOccurrence(task.getDate());
        Task next = new Task(task.getTitle(), task.getDescription(), nextDate);
        next.setPriority(task.getPriority());
        next.setRecurrence(task.getRecurrence());
        next.setOrder(nextOrderForDate(nextDate));
        tasks.add(next);
        return next;
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
            persistDates(Set.of(date, next));
        }
        return unfinished.size();
    }

    /**
     * Reports every unfinished task from any previous day to {@code today}.
     * @return the number of tasks moved.
     */
    public int reportOverdueToToday(LocalDate today) {
        List<Task> overdue = getOverdueUnfinishedTasks(today);
        Set<LocalDate> affectedDates = new HashSet<>();
        for (Task t : overdue) {
            affectedDates.add(t.getDate());
        }
        affectedDates.add(today);

        int order = nextOrderForDate(today);
        for (Task t : overdue) {
            t.setDate(today);
            t.setOrder(order++);
        }
        if (!overdue.isEmpty()) {
            persistDates(affectedDates);
        }
        return overdue.size();
    }

    private int nextOrderForDate(LocalDate date) {
        return tasks.stream()
                .filter(t -> date.equals(t.getDate()))
                .mapToInt(Task::getOrder)
                .max()
                .orElse(-1) + 1;
    }

    /** Rewrites the day file for {@code date} from the current in-memory state. */
    private void persistDate(LocalDate date) {
        List<Task> forDate = tasks.stream()
                .filter(t -> date.equals(t.getDate()))
                .collect(Collectors.toList());
        repository.saveDay(date, forDate);
    }

    private void persistDates(Set<LocalDate> dates) {
        for (LocalDate date : dates) {
            persistDate(date);
        }
    }
}
