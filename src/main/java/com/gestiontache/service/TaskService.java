package com.gestiontache.service;

import com.gestiontache.model.Task;
import com.gestiontache.model.TaskStatistics;
import com.gestiontache.repository.TaskRepository;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * In-memory task list backed by {@link TaskRepository}. Every mutation is
 * persisted immediately so the local JSON file always reflects the current state.
 */
public class TaskService {

    private final TaskRepository repository;
    private final List<Task> tasks;

    public TaskService(TaskRepository repository) {
        this.repository = repository;
        this.tasks = repository.load();
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

    private int nextOrderForDate(LocalDate date) {
        return tasks.stream()
                .filter(t -> date.equals(t.getDate()))
                .mapToInt(Task::getOrder)
                .max()
                .orElse(-1) + 1;
    }

    /**
     * Completion statistics: overall totals, plus a day-by-day count of
     * completed tasks over the {@code trailingDays} days up to and including
     * {@code today} (oldest first).
     */
    public TaskStatistics computeStatistics(LocalDate today, int trailingDays) {
        int total = tasks.size();
        int completed = (int) tasks.stream().filter(Task::isCompleted).count();

        Map<LocalDate, Long> completedPerDay = new LinkedHashMap<>();
        for (int i = trailingDays - 1; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            long count = tasks.stream()
                    .filter(t -> t.isCompleted() && day.equals(t.getDate()))
                    .count();
            completedPerDay.put(day, count);
        }

        return new TaskStatistics(total, completed, completedPerDay);
    }

    private void persist() {
        repository.save(tasks);
    }
}
