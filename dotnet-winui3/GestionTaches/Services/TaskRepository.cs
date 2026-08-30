using System.Text.Json;
using System.Text.Json.Serialization;
using GestionTaches.Models;

namespace GestionTaches.Services;

/// <summary>
/// Persists tasks locally, one JSON file per day (named "yyyy-MM-dd.json")
/// under ~/.gestion-tache/days — the exact same data directory and file
/// format used by the JavaFX client, so either client can read the other's
/// files. No server, no database.
/// </summary>
public class TaskRepository
{
    private static readonly string DefaultDataDir = Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.UserProfile), ".gestion-tache", "days");

    // namingPolicy: null keeps the raw enum member name ("HAUTE"), matching
    // Jackson's default enum serialization on the JavaFX side exactly.
    private static readonly JsonSerializerOptions JsonOptions = new()
    {
        PropertyNamingPolicy = JsonNamingPolicy.CamelCase,
        WriteIndented = true,
        Converters = { new JsonStringEnumConverter<Priority>(namingPolicy: null, allowIntegerValues: false) },
    };

    private readonly string _dataDir;

    public TaskRepository() : this(DefaultDataDir)
    {
    }

    public TaskRepository(string dataDir)
    {
        _dataDir = dataDir;
    }

    /// <summary>Loads every task from every day file in the data directory.</summary>
    public List<TaskItem> LoadAll()
    {
        var all = new List<TaskItem>();
        if (!Directory.Exists(_dataDir))
        {
            return all;
        }

        foreach (string file in Directory.EnumerateFiles(_dataDir, "*.json").OrderBy(f => f))
        {
            all.AddRange(ReadFile(file));
        }

        return all;
    }

    /// <summary>
    /// Writes the day file for <paramref name="date"/> to contain exactly
    /// <paramref name="tasksForDay"/>, or deletes it when that list is empty.
    /// </summary>
    public void SaveDay(DateOnly date, IReadOnlyList<TaskItem> tasksForDay)
    {
        string file = FileFor(date);
        if (tasksForDay.Count == 0)
        {
            if (File.Exists(file))
            {
                File.Delete(file);
            }
            return;
        }

        Directory.CreateDirectory(_dataDir);
        string json = JsonSerializer.Serialize(tasksForDay, JsonOptions);
        File.WriteAllText(file, json);
    }

    private string FileFor(DateOnly date) =>
        Path.Combine(_dataDir, date.ToString("yyyy-MM-dd") + ".json");

    private static List<TaskItem> ReadFile(string file)
    {
        string json = File.ReadAllText(file);
        var tasks = JsonSerializer.Deserialize<List<TaskItem>>(json, JsonOptions);
        return tasks ?? new List<TaskItem>();
    }
}
