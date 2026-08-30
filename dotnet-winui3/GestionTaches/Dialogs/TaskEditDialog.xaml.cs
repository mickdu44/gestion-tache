using GestionTaches.Models;
using Microsoft.UI.Xaml.Controls;

namespace GestionTaches.Dialogs;

/// <summary>Add/edit form for a task: title, description, priority and date.</summary>
public sealed partial class TaskEditDialog : ContentDialog
{
    private sealed record PriorityOption(Priority Value, string Label);

    private static readonly PriorityOption[] PriorityOptions =
        Enum.GetValues<Priority>().Select(p => new PriorityOption(p, p.ToDisplayLabel())).ToArray();

    public TaskEditDialog()
    {
        InitializeComponent();
        PriorityCombo.ItemsSource = PriorityOptions;
    }

    public void PrepareForAdd(DateOnly date)
    {
        Title = "Nouvelle tache";
        TitleBox.Text = string.Empty;
        DescriptionBox.Text = string.Empty;
        PriorityCombo.SelectedItem = PriorityOptions.First(o => o.Value == Priority.MOYENNE);
        DatePickerField.Date = date.ToDateTime(TimeOnly.MinValue);
        IsPrimaryButtonEnabled = false;
    }

    public void PrepareForEdit(TaskItem task)
    {
        Title = "Modifier la tache";
        TitleBox.Text = task.Title;
        DescriptionBox.Text = task.Description;
        PriorityCombo.SelectedItem = PriorityOptions.First(o => o.Value == task.Priority);
        DatePickerField.Date = task.Date.ToDateTime(TimeOnly.MinValue);
        IsPrimaryButtonEnabled = !string.IsNullOrWhiteSpace(task.Title);
    }

    public string TitleText => TitleBox.Text.Trim();

    public string DescriptionText => DescriptionBox.Text.Trim();

    public Priority SelectedPriority => ((PriorityOption)PriorityCombo.SelectedItem).Value;

    public DateOnly SelectedDate =>
        DateOnly.FromDateTime((DatePickerField.Date ?? DateTimeOffset.Now).DateTime);

    private void OnTitleChanged(object sender, TextChangedEventArgs e)
    {
        IsPrimaryButtonEnabled = !string.IsNullOrWhiteSpace(TitleBox.Text);
    }
}
