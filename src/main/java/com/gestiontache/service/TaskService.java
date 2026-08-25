package com.gestiontache.service;

import com.gestiontache.model.Task;
import com.gestiontache.repository.TaskRepository;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
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
                .sorted(Comparator.comparing(Task::isCompleted).thenComparing(Task::getCreatedAt))
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
        tasks.add(task);
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
        for (Task t : unfinished) {
            t.setDate(next);
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
        for (Task t : overdue) {
            t.setDate(today);
        }
        if (!overdue.isEmpty()) {
            persist();
        }
        return overdue.size();
    }

    private void persist() {
        repository.save(tasks);
    }
}
