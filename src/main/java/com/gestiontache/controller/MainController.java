package com.gestiontache.controller;

import com.gestiontache.model.Priority;
import com.gestiontache.model.Recurrence;
import com.gestiontache.model.Task;
import com.gestiontache.model.TaskStatistics;
import com.gestiontache.model.TaskStatus;
import com.gestiontache.repository.TaskRepository;
import com.gestiontache.service.TaskService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class MainController {

    private static final DateTimeFormatter DAY_FORMAT =
            DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH);
    private static final DateTimeFormatter WEEK_DAY_MONTH_FORMAT =
            DateTimeFormatter.ofPattern("d MMMM", Locale.FRENCH);
    private static final DateTimeFormatter WEEK_DAY_MONTH_YEAR_FORMAT =
            DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH);
    private static final String ALL_PRIORITIES = "Toutes priorites";

    private enum ViewMode { JOUR, SEMAINE }

    @FXML
    private ComboBox<String> priorityFilterCombo;
    @FXML
    private Label dateLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private Label countLabel;
    @FXML
    private TextField searchField;
    @FXML
    private ListView<Task> taskListView;
    @FXML
    private Button prevDayButton;
    @FXML
    private Button nextDayButton;
    @FXML
    private Button todayButton;
    @FXML
    private Button reportButton;
    @FXML
    private DatePicker datePickerNav;
    @FXML
    private ToggleButton dayViewButton;
    @FXML
    private ToggleButton weekViewButton;
    @FXML
    private ToggleGroup viewModeGroup;
    @FXML
    private CheckBox sortByPriorityCheckBox;
    @FXML
    private Label detailPlaceholder;
    @FXML
    private VBox detailContent;
    @FXML
    private TextField detailTitleField;
    @FXML
    private ComboBox<Priority> detailPriorityCombo;
    @FXML
    private ComboBox<Recurrence> detailRecurrenceCombo;
    @FXML
    private ComboBox<TaskStatus> detailStatusCombo;
    @FXML
    private DatePicker detailDatePicker;
    @FXML
    private DescriptionEditorControl detailDescriptionEditor;
    @FXML
    private SubtaskEditorControl detailSubtaskEditor;
    @FXML
    private AttachmentEditorControl detailAttachmentEditor;

    private TaskService taskService;
    private LocalDate currentDate;
    private ViewMode viewMode = ViewMode.JOUR;

    /** The task currently shown/edited in the right-hand panel, or null when none is selected. */
    private Task selectedTask;
    /** The selected task's date before an in-panel edit, so a date change can move it between days. */
    private LocalDate selectedTaskPreviousDate;
    /** Set while populating the detail panel, so programmatic field updates don't re-trigger a persist. */
    private boolean loadingDetail;

    @FXML
    private void initialize() {
        taskService = new TaskService(new TaskRepository());
        currentDate = LocalDate.now();

        priorityFilterCombo.getItems().add(ALL_PRIORITIES);
        for (Priority priority : Priority.values()) {
            priorityFilterCombo.getItems().add(priority.toString());
        }
        priorityFilterCombo.setValue(ALL_PRIORITIES);
        priorityFilterCombo.valueProperty().addListener((obs, oldValue, newValue) -> refresh());

        datePickerNav.setValue(currentDate);
        datePickerNav.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null && !newValue.equals(currentDate)) {
                currentDate = newValue;
                searchField.clear();
                refresh();
            }
        });

        viewModeGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == null) {
                viewModeGroup.selectToggle(oldToggle);
                return;
            }
            viewMode = newToggle == weekViewButton ? ViewMode.SEMAINE : ViewMode.JOUR;
            searchField.clear();
            refresh();
        });

        sortByPriorityCheckBox.selectedProperty().addListener((obs, oldValue, newValue) -> refresh());

        taskListView.setCellFactory(list -> new TaskListCell(
                this::onToggleCompleted, this::onDeleteTask,
                isSearching(), isReorderEnabled(), this::onTasksReordered));

        taskListView.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldValue, newValue) -> showTaskDetail(newValue));

        searchField.textProperty().addListener((obs, oldValue, newValue) -> refresh());

        setUpDetailPanel();

        refresh();
    }

    /** Wires the editable detail panel's controls to persist on change, guarded during population. */
    private void setUpDetailPanel() {
        detailPriorityCombo.getItems().setAll(Priority.values());
        detailRecurrenceCombo.getItems().setAll(Recurrence.values());
        detailStatusCombo.getItems().setAll(TaskStatus.values());

        detailTitleField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) {
                persistDetail(task -> task.setTitle(detailTitleField.getText().trim()));
            }
        });
        detailDescriptionEditor.getDescriptionArea().focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) {
                persistDetail(task -> task.setDescription(detailDescriptionEditor.getText()));
            }
        });
        detailPriorityCombo.valueProperty().addListener((obs, oldValue, newValue) ->
                persistDetail(task -> task.setPriority(newValue)));
        detailRecurrenceCombo.valueProperty().addListener((obs, oldValue, newValue) ->
                persistDetail(task -> task.setRecurrence(newValue)));
        detailStatusCombo.valueProperty().addListener((obs, oldValue, newValue) ->
                persistDetail(task -> task.setStatus(newValue)));
        detailDatePicker.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null) {
                persistDetail(task -> task.setDate(newValue));
            }
        });
        detailSubtaskEditor.setOnChange(() ->
                persistDetail(task -> task.setSubtasks(new ArrayList<>(detailSubtaskEditor.getSubtasks()))));
        detailAttachmentEditor.setOnChange(() ->
                persistDetail(task -> task.setAttachments(new ArrayList<>(detailAttachmentEditor.getAttachments()))));
    }

    /**
     * Applies {@code mutation} to the currently selected task and persists it,
     * unless the panel is still being populated (see {@link #loadingDetail}).
     * The list is refreshed afterwards, keeping the panel's selection and
     * focus intact.
     */
    private void persistDetail(Consumer<Task> mutation) {
        if (loadingDetail || selectedTask == null) {
            return;
        }
        mutation.accept(selectedTask);
        taskService.updateTask(selectedTask, selectedTaskPreviousDate);
        selectedTaskPreviousDate = selectedTask.getDate();
        refresh();
    }

    /**
     * Called once after the window is shown. Unfinished tasks from previous
     * days are carried forward to today automatically, with no confirmation
     * needed from the user.
     */
    public void checkOverdueTasks() {
        int moved = taskService.reportOverdueToToday(LocalDate.now());
        if (moved == 0) {
            return;
        }
        currentDate = LocalDate.now();
        searchField.clear();
        refresh();
        showInfo(moved + " tache(s) non terminee(s) reportee(s) automatiquement a aujourd'hui.");
    }

    @FXML
    private void onPreviousDay() {
        currentDate = currentDate.minusDays(viewMode == ViewMode.SEMAINE ? 7 : 1);
        searchField.clear();
        refresh();
    }

    @FXML
    private void onNextDay() {
        currentDate = currentDate.plusDays(viewMode == ViewMode.SEMAINE ? 7 : 1);
        searchField.clear();
        refresh();
    }

    @FXML
    private void onToday() {
        currentDate = LocalDate.now();
        searchField.clear();
        refresh();
    }

    @FXML
    private void onClearSearch() {
        searchField.clear();
    }

    @FXML
    private void onAddTask() {
        LocalDate defaultDate = isSearching() ? LocalDate.now() : currentDate;
        openTaskDialog(defaultDate).ifPresent(task -> {
            taskService.addTask(task);
            refresh();
        });
    }

    @FXML
    private void onReportUnfinished() {
        if (isSearching()) {
            return;
        }
        int moved = taskService.reportUnfinishedToNextDay(currentDate);
        refresh();
        if (moved == 0) {
            showInfo("Aucune tache non terminee a reporter pour ce jour.");
        } else {
            showInfo(moved + " tache(s) reportee(s) au lendemain.");
        }
    }

    @FXML
    private void onShowStatistics() {
        TaskStatistics stats = taskService.computeStatistics(LocalDate.now(), 7);

        Label totalLabel = new Label("Total : " + stats.totalTasks() + " tache(s)");
        Label completedLabel = new Label(String.format(Locale.FRENCH, "Terminees : %d (%.0f%%)",
                stats.completedTasks(), stats.completionRate() * 100));
        totalLabel.getStyleClass().add("stats-summary");
        completedLabel.getStyleClass().add("stats-summary");

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        yAxis.setForceZeroInRange(true);
        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setTitle("Taches terminees - 7 derniers jours");
        chart.setLegendVisible(false);
        chart.setAnimated(false);

        DateTimeFormatter dayFormat = DateTimeFormatter.ofPattern("EEE dd/MM", Locale.FRENCH);
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (Map.Entry<LocalDate, Long> entry : stats.completedPerDay().entrySet()) {
            series.getData().add(new XYChart.Data<>(capitalize(entry.getKey().format(dayFormat)), entry.getValue()));
        }
        chart.getData().add(series);

        VBox content = new VBox(12, totalLabel, completedLabel, chart);
        content.setPadding(new Insets(16));
        content.setPrefWidth(480);
        content.setPrefHeight(380);

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Statistiques");
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/com/gestiontache/style.css").toExternalForm());
        dialog.showAndWait();
    }

    private void onToggleCompleted(Task task, boolean completed) {
        taskService.setCompleted(task, completed);
        refresh();
    }

    private void onDeleteTask(Task task) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Supprimer la tache");
        alert.setHeaderText("Supprimer \"" + task.getTitle() + "\" ?");
        alert.setContentText("Cette action est definitive.");
        alert.getDialogPane().getStylesheets().add(getClass().getResource("/com/gestiontache/style.css").toExternalForm());
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            taskService.deleteTask(task);
            refresh();
        }
    }

    /** Called after a drag-and-drop move within the day view; persists the new manual order. */
    private void onTasksReordered() {
        if (!isReorderEnabled()) {
            return;
        }
        taskService.reorderTasksForDate(currentDate, taskListView.getItems());
    }

    /**
     * Manual reordering only makes sense when browsing a single day with no
     * priority filter applied and no priority sort active (otherwise the
     * list view holds a subset or a recomputed order of the day's tasks,
     * and a drag-and-drop move could not be persisted meaningfully).
     */
    private boolean isReorderEnabled() {
        return !isSearching() && viewMode == ViewMode.JOUR && ALL_PRIORITIES.equals(priorityFilterCombo.getValue())
                && !sortByPriorityCheckBox.isSelected();
    }

    /** The date badge (which day a task belongs to) is only useful when a single day isn't the whole view. */
    private boolean showDateBadge() {
        return isSearching() || viewMode == ViewMode.SEMAINE;
    }

    /** Opens the dialog to create a new task, returned on confirmation. Editing happens inline in the detail panel. */
    private Optional<Task> openTaskDialog(LocalDate defaultDate) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/gestiontache/task-dialog.fxml"));
            DialogPane pane = loader.load();
            TaskDialogController controller = loader.getController();
            controller.fill(null, null, Priority.MOYENNE, Recurrence.AUCUNE, TaskStatus.A_FAIRE, defaultDate, null, null);

            Dialog<Task> dialog = new Dialog<>();
            dialog.setTitle("Nouvelle tache");
            dialog.setDialogPane(pane);

            Button okButton = (Button) pane.lookupButton(ButtonType.OK);
            okButton.disableProperty().bind(controller.getTitleField().textProperty().isEmpty());

            dialog.setResultConverter(buttonType -> {
                if (buttonType != ButtonType.OK) {
                    return null;
                }
                Task task = new Task(controller.getTitle(), controller.getDescription(), controller.getDate());
                task.setPriority(controller.getPriority());
                task.setRecurrence(controller.getRecurrence());
                task.setStatus(controller.getStatus());
                task.setSubtasks(controller.getSubtasks());
                task.setAttachments(controller.getAttachments());
                return task;
            });

            return dialog.showAndWait();
        } catch (IOException e) {
            throw new RuntimeException("Impossible d'ouvrir la fenetre de tache", e);
        }
    }

    private boolean isSearching() {
        return searchField != null && !searchField.getText().isBlank();
    }

    private void refresh() {
        boolean searching = isSearching();
        boolean weekView = viewMode == ViewMode.SEMAINE;
        prevDayButton.setDisable(searching);
        nextDayButton.setDisable(searching);
        todayButton.setDisable(searching);
        reportButton.setDisable(searching || weekView);
        datePickerNav.setDisable(searching);
        dayViewButton.setDisable(searching);
        weekViewButton.setDisable(searching);
        if (!searching) {
            datePickerNav.setValue(currentDate);
        }

        taskListView.setCellFactory(list -> new TaskListCell(
                this::onToggleCompleted, this::onDeleteTask,
                showDateBadge(), isReorderEnabled(), this::onTasksReordered));

        LocalDate weekStart = currentDate.with(DayOfWeek.MONDAY);
        LocalDate weekEnd = weekStart.plusDays(6);

        List<Task> tasks;
        if (searching) {
            String keyword = searchField.getText();
            tasks = taskService.search(keyword);
        } else if (weekView) {
            tasks = new ArrayList<>();
            for (int i = 0; i < 7; i++) {
                tasks.addAll(taskService.getTasksForDate(weekStart.plusDays(i)));
            }
        } else {
            tasks = taskService.getTasksForDate(currentDate);
        }

        String priorityFilter = priorityFilterCombo.getValue();
        if (priorityFilter != null && !ALL_PRIORITIES.equals(priorityFilter)) {
            tasks = tasks.stream()
                    .filter(t -> priorityFilter.equals(t.getPriority().toString()))
                    .collect(Collectors.toList());
        }

        if (sortByPriorityCheckBox.isSelected()) {
            // Stable sort: completed tasks stay last, priority order (Haute
            // first) applies within each of the two groups.
            tasks = tasks.stream()
                    .sorted(Comparator.comparing(Task::isCompleted).thenComparing(Task::getPriority))
                    .collect(Collectors.toList());
        }

        if (searching) {
            String keyword = searchField.getText();
            dateLabel.setText("Resultats de recherche");
            statusLabel.setText("\"" + keyword.trim() + "\" trouve dans le titre ou la description");
            countLabel.setText(tasks.size() + " tache(s) trouvee(s)");
        } else if (weekView) {
            String label = formatWeekLabel(weekStart, weekEnd);
            LocalDate today = LocalDate.now();
            if (!today.isBefore(weekStart) && !today.isAfter(weekEnd)) {
                label += " (semaine en cours)";
            }
            dateLabel.setText(label);
            statusLabel.setText("");
            long done = tasks.stream().filter(Task::isCompleted).count();
            countLabel.setText(done + " / " + tasks.size() + " tache(s) terminee(s)");
        } else {
            String label = capitalize(currentDate.format(DAY_FORMAT));
            if (currentDate.isEqual(LocalDate.now())) {
                label += " (aujourd'hui)";
            }
            dateLabel.setText(label);
            statusLabel.setText("");
            long done = tasks.stream().filter(Task::isCompleted).count();
            countLabel.setText(done + " / " + tasks.size() + " tache(s) terminee(s)");
        }

        Task previouslySelected = taskListView.getSelectionModel().getSelectedItem();
        String previouslySelectedId = previouslySelected != null ? previouslySelected.getId() : null;

        taskListView.getItems().setAll(tasks);

        if (previouslySelectedId != null) {
            tasks.stream()
                    .filter(t -> previouslySelectedId.equals(t.getId()))
                    .findFirst()
                    .ifPresentOrElse(
                            t -> taskListView.getSelectionModel().select(t),
                            () -> showTaskDetail(null));
        } else {
            showTaskDetail(null);
        }
    }

    /** Populates the editable right-hand panel with the given task's detail, or shows a placeholder when null. */
    private void showTaskDetail(Task task) {
        boolean hasSelection = task != null;
        detailPlaceholder.setVisible(!hasSelection);
        detailPlaceholder.setManaged(!hasSelection);
        detailContent.setVisible(hasSelection);
        detailContent.setManaged(hasSelection);

        selectedTask = task;
        if (!hasSelection) {
            return;
        }
        selectedTaskPreviousDate = task.getDate();

        loadingDetail = true;
        try {
            detailTitleField.setText(task.getTitle());
            detailPriorityCombo.setValue(task.getPriority());
            detailRecurrenceCombo.setValue(task.getRecurrence());
            detailStatusCombo.setValue(task.getStatus());
            detailDatePicker.setValue(task.getDate());
            detailDescriptionEditor.setText(task.getDescription());
            detailSubtaskEditor.setSubtasks(task.getSubtasks());
            detailAttachmentEditor.setAttachments(task.getAttachments());
        } finally {
            loadingDetail = false;
        }
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Gestion des taches");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getDialogPane().getStylesheets().add(getClass().getResource("/com/gestiontache/style.css").toExternalForm());
        alert.showAndWait();
    }

    private static String capitalize(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    /** "Semaine du 24 au 30 aout 2026", or "Semaine du 29 aout au 4 septembre 2026" across a month boundary. */
    private static String formatWeekLabel(LocalDate weekStart, LocalDate weekEnd) {
        String start = weekStart.getMonth() == weekEnd.getMonth()
                ? String.valueOf(weekStart.getDayOfMonth())
                : weekStart.format(WEEK_DAY_MONTH_FORMAT);
        return "Semaine du " + start + " au " + weekEnd.format(WEEK_DAY_MONTH_YEAR_FORMAT);
    }
}
