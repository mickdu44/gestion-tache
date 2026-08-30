using System.Globalization;
using GestionTaches.Models;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Media;

namespace GestionTaches;

/// <summary>
/// Read-only display wrapper around a <see cref="TaskItem"/> for one row of
/// the task ListView. Rebuilt fresh on every refresh, so no change
/// notification is needed on these properties.
/// </summary>
public class TaskRow
{
    private static readonly CultureInfo French = new("fr-FR");


    public required TaskItem Task { get; init; }

    public required bool ShowDate { get; init; }

    public string DescriptionPreview
    {
        get
        {
            if (string.IsNullOrWhiteSpace(Task.Description))
            {
                return string.Empty;
            }

            int newline = Task.Description.IndexOf('\n');
            return newline >= 0 ? Task.Description[..newline] + " …" : Task.Description;
        }
    }

    public Visibility DescriptionVisibility =>
        string.IsNullOrWhiteSpace(Task.Description) ? Visibility.Collapsed : Visibility.Visible;

    public double CompletedOpacity => Task.Completed ? 0.55 : 1.0;

    public string PriorityLabel => Task.Priority.ToDisplayLabel();

    public Brush PriorityBackground => Task.Priority switch
    {
        Priority.HAUTE => (Brush)Application.Current.Resources["PriorityHauteBrush"],
        Priority.MOYENNE => (Brush)Application.Current.Resources["PriorityMoyenneBrush"],
        _ => (Brush)Application.Current.Resources["PriorityBasseBrush"],
    };

    public Brush PriorityForeground => Task.Priority switch
    {
        Priority.HAUTE => (Brush)Application.Current.Resources["PriorityHauteTextBrush"],
        Priority.MOYENNE => (Brush)Application.Current.Resources["PriorityMoyenneTextBrush"],
        _ => (Brush)Application.Current.Resources["PriorityBasseTextBrush"],
    };

    public Visibility DateBadgeVisibility => ShowDate ? Visibility.Visible : Visibility.Collapsed;

    public string DateBadgeText => Task.Date.ToString("d MMM yyyy", French);
}
