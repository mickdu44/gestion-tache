using GestionTaches.Models;

namespace GestionTaches.Services;

/// <summary>
/// In-memory task list backed by <see cref="TaskRepository"/>. Every
/// mutation is persisted immediately, rewriting only the day file(s)
/// actually affected. Mirrors the JavaFX client's TaskService (core MVP
/// subset: no recurrence/subtasks/attachments/statistics here).
/// </summary>
public class TaskService
{
    private readonly TaskRepository _repository;
    private readonly List<TaskItem> _tasks;

    public TaskService(TaskRepository repository)
    {
        _repository = repository;
        _tasks = repository.LoadAll();
    }

    public List<TaskItem> GetTasksForDate(DateOnly date) =>
        _tasks.Where(t => t.Date == date)
            .OrderBy(t => t.Order)
            .ThenBy(t => t.CreatedAt)
            .ToList();

    /// <summary>Searches every task, across every day file, by title or description.</summary>
    public List<TaskItem> Search(string? keyword) =>
        _tasks.Where(t => t.Matches(keyword))
            .OrderByDescending(t => t.Date)
            .ToList();

    public List<TaskItem> GetOverdueUnfinishedTasks(DateOnly today) =>
        _tasks.Where(t => !t.Completed && t.Date < today).ToList();

    public void AddTask(TaskItem task)
    {
        task.Order = NextOrderForDate(task.Date);
        _tasks.Add(task);
        PersistDate(task.Date);
    }

    /// <summary>
    /// The task object is mutated in place by the caller (including its
    /// date, for an edit that moves it to another day); this persists
    /// whichever day file(s) are actually affected.
    /// </summary>
    public void UpdateTask(TaskItem task, DateOnly previousDate)
    {
        if (previousDate == task.Date)
        {
            PersistDate(task.Date);
        }
        else
        {
            PersistDates(new HashSet<DateOnly> { previousDate, task.Date });
        }
    }

    public void DeleteTask(TaskItem task)
    {
        DateOnly date = task.Date;
        _tasks.Remove(task);
        PersistDate(date);
    }

    public void SetCompleted(TaskItem task, bool completed)
    {
        task.Completed = completed;
        PersistDate(task.Date);
    }

    /// <summary>Reports every unfinished task of <paramref name="date"/> to the next day. Returns the number of tasks moved.</summary>
    public int ReportUnfinishedToNextDay(DateOnly date)
    {
        List<TaskItem> unfinished = _tasks.Where(t => t.Date == date && !t.Completed).ToList();
        DateOnly next = date.AddDays(1);
        int order = NextOrderForDate(next);
        foreach (TaskItem t in unfinished)
        {
            t.Date = next;
            t.Order = order++;
        }

        if (unfinished.Count > 0)
        {
            PersistDates(new HashSet<DateOnly> { date, next });
        }

        return unfinished.Count;
    }

    /// <summary>Reports every unfinished task from any previous day to <paramref name="today"/>. Returns the number of tasks moved.</summary>
    public int ReportOverdueToToday(DateOnly today)
    {
        List<TaskItem> overdue = GetOverdueUnfinishedTasks(today);
        var affectedDates = new HashSet<DateOnly>();
        foreach (TaskItem t in overdue)
        {
            affectedDates.Add(t.Date);
        }
        affectedDates.Add(today);

        int order = NextOrderForDate(today);
        foreach (TaskItem t in overdue)
        {
            t.Date = today;
            t.Order = order++;
        }

        if (overdue.Count > 0)
        {
            PersistDates(affectedDates);
        }

        return overdue.Count;
    }

    private int NextOrderForDate(DateOnly date)
    {
        var ordersForDate = _tasks.Where(t => t.Date == date).Select(t => t.Order).ToList();
        return (ordersForDate.Count > 0 ? ordersForDate.Max() : -1) + 1;
    }

    /// <summary>Rewrites the day file for <paramref name="date"/> from the current in-memory state.</summary>
    private void PersistDate(DateOnly date)
    {
        List<TaskItem> forDate = _tasks.Where(t => t.Date == date).ToList();
        _repository.SaveDay(date, forDate);
    }

    private void PersistDates(IEnumerable<DateOnly> dates)
    {
        foreach (DateOnly date in dates)
        {
            PersistDate(date);
        }
    }
}
