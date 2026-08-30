using System.Text.Json;
using System.Text.Json.Serialization;

namespace GestionTaches.Models;

/// <summary>
/// A single work task attached to a given day. Field names and JSON shape
/// mirror the JavaFX client's <c>Task</c> model exactly (same
/// ~/.gestion-tache/days/*.json files), so both clients can read the same
/// data directory. Fields the JavaFX client has grown since (recurrence,
/// subtasks, attachments) aren't edited here, but <see cref="ExtraData"/>
/// round-trips them untouched instead of silently discarding them.
/// </summary>
public class TaskItem
{
    public string Id { get; set; } = Guid.NewGuid().ToString();

    public string Title { get; set; } = string.Empty;

    public string Description { get; set; } = string.Empty;

    public DateOnly Date { get; set; }

    public bool Completed { get; set; }

    public DateTime CreatedAt { get; set; } = DateTime.Now;

    /// <summary>Manual rank among the tasks of the same day, used for drag-and-drop ordering in the JavaFX client.</summary>
    public int Order { get; set; }

    public Priority Priority { get; set; } = Priority.MOYENNE;

    /// <summary>
    /// Any JSON property this client doesn't know about yet (recurrence,
    /// subtasks, attachments...), preserved so saving a day file never
    /// drops data written by the JavaFX client.
    /// </summary>
    [JsonExtensionData]
    public Dictionary<string, JsonElement>? ExtraData { get; set; }

    /// <summary>
    /// A task matches a keyword when it appears (case-insensitive) in the
    /// title or the description, matching the JavaFX client's Task.matches().
    /// </summary>
    public bool Matches(string? keyword)
    {
        if (string.IsNullOrWhiteSpace(keyword))
        {
            return true;
        }

        string lower = keyword.Trim().ToLowerInvariant();
        bool inTitle = Title.ToLowerInvariant().Contains(lower);
        bool inDescription = Description.ToLowerInvariant().Contains(lower);
        return inTitle || inDescription;
    }
}
