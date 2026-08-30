namespace GestionTaches.Models;

/// <summary>Priority level of a task, from most to least urgent.</summary>
public enum Priority
{
    HAUTE,
    MOYENNE,
    BASSE,
}

public static class PriorityExtensions
{
    /// <summary>French display label, matching the Java client's Priority.toString().</summary>
    public static string ToDisplayLabel(this Priority priority) => priority switch
    {
        Priority.HAUTE => "Haute",
        Priority.MOYENNE => "Moyenne",
        Priority.BASSE => "Basse",
        _ => priority.ToString(),
    };
}
