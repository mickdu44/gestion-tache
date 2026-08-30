using System.Globalization;
using GestionTaches.Dialogs;
using GestionTaches.Models;
using GestionTaches.Services;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;

namespace GestionTaches;

public sealed partial class MainWindow : Window
{
    private const string AllPriorities = "Toutes priorites";
    private static readonly CultureInfo French = new("fr-FR");

    private readonly TaskService _taskService;
    private DateOnly _currentDate;
    private bool _overdueChecked;

    public MainWindow()
    {
        InitializeComponent();
        Title = "Gestion des taches";

        _taskService = new TaskService(new TaskRepository());
        _currentDate = DateOnly.FromDateTime(DateTime.Now);

        PriorityFilterCombo.Items.Add(AllPriorities);
        foreach (Priority priority in Enum.GetValues<Priority>())
        {
            PriorityFilterCombo.Items.Add(priority.ToDisplayLabel());
        }
        PriorityFilterCombo.SelectedItem = AllPriorities;

        DatePickerNav.Date = _currentDate.ToDateTime(TimeOnly.MinValue);

        Refresh();

        Activated += OnWindowActivated;
    }

    /// <summary>
    /// Runs once after the window is shown: unfinished tasks from previous
    /// days are carried forward to today automatically, no confirmation
    /// needed from the user.
    /// </summary>
    private void OnWindowActivated(object sender, WindowActivatedEventArgs args)
    {
        if (_overdueChecked)
        {
            return;
        }
        _overdueChecked = true;

        int moved = _taskService.ReportOverdueToToday(DateOnly.FromDateTime(DateTime.Now));
        if (moved == 0)
        {
            return;
        }

        _currentDate = DateOnly.FromDateTime(DateTime.Now);
        SearchField.Text = string.Empty;
        Refresh();
        _ = ShowInfoAsync($"{moved} tache(s) non terminee(s) reportee(s) automatiquement a aujourd'hui.");
    }

    private void OnPreviousDay(object sender, RoutedEventArgs e)
    {
        _currentDate = _currentDate.AddDays(-1);
        SearchField.Text = string.Empty;
        Refresh();
    }

    private void OnNextDay(object sender, RoutedEventArgs e)
    {
        _currentDate = _currentDate.AddDays(1);
        SearchField.Text = string.Empty;
        Refresh();
    }

    private void OnToday(object sender, RoutedEventArgs e)
    {
        _currentDate = DateOnly.FromDateTime(DateTime.Now);
        SearchField.Text = string.Empty;
        Refresh();
    }

    private void OnDatePicked(CalendarDatePicker sender, CalendarDatePickerDateChangedEventArgs args)
    {
        if (args.NewDate is not DateTimeOffset newDate)
        {
            return;
        }

        var picked = DateOnly.FromDateTime(newDate.DateTime);
        if (picked == _currentDate)
        {
            return;
        }

        _currentDate = picked;
        SearchField.Text = string.Empty;
        Refresh();
    }

    private void OnSearchTextChanged(object sender, TextChangedEventArgs e) => Refresh();

    private void OnClearSearch(object sender, RoutedEventArgs e) => SearchField.Text = string.Empty;

    private void OnPriorityFilterChanged(object sender, SelectionChangedEventArgs e) => Refresh();

    private async void OnAddTask(object sender, RoutedEventArgs e)
    {
        DateOnly defaultDate = IsSearching() ? DateOnly.FromDateTime(DateTime.Now) : _currentDate;
        var dialog = new TaskEditDialog { XamlRoot = Content.XamlRoot };
        dialog.PrepareForAdd(defaultDate);

        if (await dialog.ShowAsync() != ContentDialogResult.Primary)
        {
            return;
        }

        var task = new TaskItem
        {
            Title = dialog.TitleText,
            Description = dialog.DescriptionText,
            Priority = dialog.SelectedPriority,
            Date = dialog.SelectedDate,
        };
        _taskService.AddTask(task);
        Refresh();
    }

    private async void OnEditTask(object sender, RoutedEventArgs e)
    {
        var task = ((TaskRow)((Button)sender).Tag).Task;
        DateOnly previousDate = task.Date;

        var dialog = new TaskEditDialog { XamlRoot = Content.XamlRoot };
        dialog.PrepareForEdit(task);

        if (await dialog.ShowAsync() != ContentDialogResult.Primary)
        {
            return;
        }

        task.Title = dialog.TitleText;
        task.Description = dialog.DescriptionText;
        task.Priority = dialog.SelectedPriority;
        task.Date = dialog.SelectedDate;
        _taskService.UpdateTask(task, previousDate);
        Refresh();
    }

    private async void OnDeleteTask(object sender, RoutedEventArgs e)
    {
        var task = ((TaskRow)((Button)sender).Tag).Task;

        var confirm = new ContentDialog
        {
            XamlRoot = Content.XamlRoot,
            Title = $"Supprimer \"{task.Title}\" ?",
            Content = "Cette action est definitive.",
            PrimaryButtonText = "Supprimer",
            CloseButtonText = "Annuler",
            DefaultButton = ContentDialogButton.Close,
        };

        if (await confirm.ShowAsync() != ContentDialogResult.Primary)
        {
            return;
        }

        _taskService.DeleteTask(task);
        Refresh();
    }

    private void OnTaskChecked(object sender, RoutedEventArgs e)
    {
        var row = (TaskRow)((CheckBox)sender).Tag;
        _taskService.SetCompleted(row.Task, ((CheckBox)sender).IsChecked ?? false);
        Refresh();
    }

    private async void OnReportUnfinished(object sender, RoutedEventArgs e)
    {
        if (IsSearching())
        {
            return;
        }

        int moved = _taskService.ReportUnfinishedToNextDay(_currentDate);
        Refresh();
        await ShowInfoAsync(moved == 0
            ? "Aucune tache non terminee a reporter pour ce jour."
            : $"{moved} tache(s) reportee(s) au lendemain.");
    }

    private bool IsSearching() => !string.IsNullOrWhiteSpace(SearchField.Text);

    private void Refresh()
    {
        bool searching = IsSearching();
        PrevDayButton.IsEnabled = !searching;
        NextDayButton.IsEnabled = !searching;
        TodayButton.IsEnabled = !searching;
        DatePickerNav.IsEnabled = !searching;
        ReportButton.IsEnabled = !searching;
        if (!searching)
        {
            DatePickerNav.Date = _currentDate.ToDateTime(TimeOnly.MinValue);
        }

        List<TaskItem> tasks = searching
            ? _taskService.Search(SearchField.Text)
            : _taskService.GetTasksForDate(_currentDate);

        string? priorityFilter = PriorityFilterCombo.SelectedItem as string;
        if (priorityFilter != null && priorityFilter != AllPriorities)
        {
            tasks = tasks.Where(t => t.Priority.ToDisplayLabel() == priorityFilter).ToList();
        }

        if (searching)
        {
            DateLabel.Text = "Resultats de recherche";
            StatusLabel.Text = $"\"{SearchField.Text.Trim()}\" trouve dans le titre ou la description";
            CountLabel.Text = $"{tasks.Count} tache(s) trouvee(s)";
        }
        else
        {
            string label = Capitalize(_currentDate.ToDateTime(TimeOnly.MinValue)
                .ToString("dddd d MMMM yyyy", French));
            if (_currentDate == DateOnly.FromDateTime(DateTime.Now))
            {
                label += " (aujourd'hui)";
            }
            DateLabel.Text = label;
            StatusLabel.Text = string.Empty;
            int done = tasks.Count(t => t.Completed);
            CountLabel.Text = $"{done} / {tasks.Count} tache(s) terminee(s)";
        }

        TaskListView.ItemsSource = tasks
            .Select(t => new TaskRow { Task = t, ShowDate = searching })
            .ToList();
    }

    private async Task ShowInfoAsync(string message)
    {
        var dialog = new ContentDialog
        {
            XamlRoot = Content.XamlRoot,
            Title = "Gestion des taches",
            Content = message,
            CloseButtonText = "OK",
        };
        await dialog.ShowAsync();
    }

    private static string Capitalize(string text) =>
        string.IsNullOrEmpty(text) ? text : char.ToUpperInvariant(text[0]) + text[1..];
}
