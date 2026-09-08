package com.gestiontache.controller;

import com.gestiontache.model.HistoryEntry;
import com.gestiontache.model.Priority;
import com.gestiontache.model.Recurrence;
import com.gestiontache.model.SubTask;
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
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.File;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MainController {

    private static final DateTimeFormatter DAY_FORMAT =
            DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH);
    private static final DateTimeFormatter WEEK_DAY_MONTH_FORMAT =
            DateTimeFormatter.ofPattern("d MMMM", Locale.FRENCH);
    private static final DateTimeFormatter WEEK_DAY_MONTH_YEAR_FORMAT =
            DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH);
    private static final DateTimeFormatter HISTORY_DATE_FORMAT =
            DateTimeFormatter.ofPattern("d MMM yyyy", Locale.FRENCH);
    private static final DateTimeFormatter HISTORY_TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.FRENCH);
    private static final String ALL_PRIORITIES = "Toutes priorites";

    private enum ViewMode { JOUR, SEMAINE, KANBAN }

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
    private ToggleButton kanbanViewButton;
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
    @FXML
    private ListView<String> detailHistoryList;
    @FXML
    private HBox kanbanBoard;
    @FXML
    private Label kanbanTodoHeader;
    @FXML
    private ListView<Task> kanbanTodoList;
    @FXML
    private Label kanbanInProgressHeader;
    @FXML
    private ListView<Task> kanbanInProgressList;
    @FXML
    private Label kanbanDoneHeader;
    @FXML
    private ListView<Task> kanbanDoneList;

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
            if (newToggle == weekViewButton) {
                viewMode = ViewMode.SEMAINE;
            } else if (newToggle == kanbanViewButton) {
                viewMode = ViewMode.KANBAN;
            } else {
                viewMode = ViewMode.JOUR;
            }
            searchField.clear();
            refresh();
        });

        sortByPriorityCheckBox.selectedProperty().addListener((obs, oldValue, newValue) -> refresh());

        taskListView.setCellFactory(list -> new TaskListCell(
                this::onToggleCompleted, this::onDeleteTask,
                isSearching(), isReorderEnabled(), this::onTasksReordered));

        taskListView.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldValue, newValue) -> showTaskDetail(newValue));

        // Kanban columns use a compact card (KanbanTaskCell) instead of
        // TaskListCell's wide row, which doesn't fit a narrow column.
        for (ListView<Task> kanbanList : List.of(kanbanTodoList, kanbanInProgressList, kanbanDoneList)) {
            kanbanList.setCellFactory(list -> new KanbanTaskCell(this::onToggleCompleted, this::onDeleteTask));
            kanbanList.getSelectionModel().selectedItemProperty()
                    .addListener((obs, oldValue, newValue) -> showTaskDetail(newValue));
        }

        searchField.textProperty().addListener((obs, oldValue, newValue) -> refresh());

        setUpDetailPanel();

        refresh();
    }

    /** Wires the editable detail panel's controls to persist on change, guarded during population. */
    private void setUpDetailPanel() {
        detailPriorityCombo.getItems().setAll(Priority.values());
        detailRecurrenceCombo.getItems().setAll(Recurrence.values());
        detailStatusCombo.getItems().setAll(TaskStatus.values());
        detailHistoryList.setPlaceholder(new Label("Aucun changement enregistre."));

        detailTitleField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) {
                String newTitle = detailTitleField.getText().trim();
                persistDetail(task -> task.setTitle(newTitle),
                        task -> titleChangeMessage(task.getTitle(), newTitle));
            }
        });
        detailDescriptionEditor.getDescriptionArea().focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) {
                String newDescription = detailDescriptionEditor.getText();
                persistDetail(task -> task.setDescription(newDescription),
                        task -> Objects.equals(task.getDescription(), newDescription) ? null : "Description modifiee");
            }
        });
        detailPriorityCombo.valueProperty().addListener((obs, oldValue, newValue) ->
                persistDetail(task -> task.setPriority(newValue),
                        task -> "Priorite changee : " + oldValue + " -> " + newValue));
        detailRecurrenceCombo.valueProperty().addListener((obs, oldValue, newValue) ->
                persistDetail(task -> task.setRecurrence(newValue),
                        task -> "Recurrence changee : " + oldValue + " -> " + newValue));
        detailStatusCombo.valueProperty().addListener((obs, oldValue, newValue) ->
                persistDetail(task -> task.setStatus(newValue),
                        task -> "Statut change : " + oldValue + " -> " + newValue));
        detailDatePicker.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null) {
                persistDetail(task -> task.setDate(newValue),
                        task -> (oldValue == null || oldValue.equals(newValue)) ? null
                                : "Date deplacee : " + oldValue.format(HISTORY_DATE_FORMAT)
                                        + " -> " + newValue.format(HISTORY_DATE_FORMAT));
            }
        });
        detailSubtaskEditor.setOnChange(() -> {
            List<SubTask> newSubtasks = new ArrayList<>(detailSubtaskEditor.getSubtasks());
            persistDetail(task -> task.setSubtasks(newSubtasks),
                    task -> describeSubtaskChange(task.getSubtasks(), newSubtasks));
        });
        detailAttachmentEditor.setOnChange(() -> {
            List<String> newAttachments = new ArrayList<>(detailAttachmentEditor.getAttachments());
            persistDetail(task -> task.setAttachments(newAttachments),
                    task -> describeAttachmentChange(task.getAttachments(), newAttachments));
        });
    }

    /**
     * Applies {@code mutation} to the currently selected task and persists it,
     * unless the panel is still being populated (see {@link #loadingDetail}).
     * {@code historyMessage}, if given, is evaluated against the task's state
     * just before the mutation and, when non-null, appended to its history.
     * The list is refreshed afterwards, keeping the panel's selection and
     * focus intact.
     */
    private void persistDetail(Consumer<Task> mutation, Function<Task, String> historyMessage) {
        if (loadingDetail || selectedTask == null) {
            return;
        }
        String message = historyMessage != null ? historyMessage.apply(selectedTask) : null;
        mutation.accept(selectedTask);
        if (message != null) {
            selectedTask.addHistoryEntry(message);
        }
        taskService.updateTask(selectedTask, selectedTaskPreviousDate);
        selectedTaskPreviousDate = selectedTask.getDate();
        refresh();
    }

    private static String titleChangeMessage(String oldTitle, String newTitle) {
        if (Objects.equals(oldTitle, newTitle)) {
            return null;
        }
        return "Titre modifie : \"" + oldTitle + "\" -> \"" + newTitle + "\"";
    }

    /** Describes the single add/remove/toggle that just happened, matching subtasks by their stable id. */
    private static String describeSubtaskChange(List<SubTask> oldList, List<SubTask> newList) {
        Map<String, SubTask> oldById = new HashMap<>();
        for (SubTask s : oldList) {
            oldById.put(s.getId(), s);
        }
        Map<String, SubTask> newById = new HashMap<>();
        for (SubTask s : newList) {
            newById.put(s.getId(), s);
        }
        for (SubTask s : newList) {
            if (!oldById.containsKey(s.getId())) {
                return "Sous-tache ajoutee : \"" + s.getTitle() + "\"";
            }
        }
        for (SubTask s : oldList) {
            if (!newById.containsKey(s.getId())) {
                return "Sous-tache supprimee : \"" + s.getTitle() + "\"";
            }
        }
        for (SubTask s : newList) {
            SubTask previous = oldById.get(s.getId());
            if (previous != null && previous.isCompleted() != s.isCompleted()) {
                return "Sous-tache " + (s.isCompleted() ? "cochee" : "decochee") + " : \"" + s.getTitle() + "\"";
            }
        }
        return null;
    }

    /** Describes the single add/remove that just happened, matching attachments by their file path. */
    private static String describeAttachmentChange(List<String> oldList, List<String> newList) {
        for (String path : newList) {
            if (!oldList.contains(path)) {
                return "Piece jointe ajoutee : \"" + new File(path).getName() + "\"";
            }
        }
        for (String path : oldList) {
            if (!newList.contains(path)) {
                return "Piece jointe supprimee : \"" + new File(path).getName() + "\"";
            }
        }
        return null;
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
        boolean kanbanView = viewMode == ViewMode.KANBAN;
        prevDayButton.setDisable(searching);
        nextDayButton.setDisable(searching);
        todayButton.setDisable(searching);
        reportButton.setDisable(searching || weekView);
        datePickerNav.setDisable(searching);
        dayViewButton.setDisable(searching);
        weekViewButton.setDisable(searching);
        kanbanViewButton.setDisable(searching);
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

        String previouslySelectedId = selectedTask != null ? selectedTask.getId() : null;

        taskListView.setVisible(!kanbanView);
        taskListView.setManaged(!kanbanView);
        kanbanBoard.setVisible(kanbanView);
        kanbanBoard.setManaged(kanbanView);

        if (kanbanView) {
            showKanbanBoard(tasks, previouslySelectedId);
        } else {
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
    }

    /**
     * Splits {@code tasks} (the current day's tasks, already filtered/sorted
     * like the flat list) into the three status columns. All three lists are
     * repopulated before re-selecting, so only the one re-selection (if any)
     * has the final say on what the detail panel shows.
     */
    private void showKanbanBoard(List<Task> tasks, String previouslySelectedId) {
        List<Task> todo = tasks.stream().filter(t -> t.getStatus() == TaskStatus.A_FAIRE).collect(Collectors.toList());
        List<Task> inProgress = tasks.stream().filter(t -> t.getStatus() == TaskStatus.EN_COURS).collect(Collectors.toList());
        List<Task> done = tasks.stream().filter(t -> t.getStatus() == TaskStatus.TERMINEE).collect(Collectors.toList());

        kanbanTodoHeader.setText("A faire (" + todo.size() + ")");
        kanbanInProgressHeader.setText("En cours (" + inProgress.size() + ")");
        kanbanDoneHeader.setText("Terminee (" + done.size() + ")");

        kanbanTodoList.getItems().setAll(todo);
        kanbanInProgressList.getItems().setAll(inProgress);
        kanbanDoneList.getItems().setAll(done);

        boolean reselected = previouslySelectedId != null
                && (selectIfPresent(kanbanTodoList, todo, previouslySelectedId)
                        || selectIfPresent(kanbanInProgressList, inProgress, previouslySelectedId)
                        || selectIfPresent(kanbanDoneList, done, previouslySelectedId));
        if (!reselected) {
            showTaskDetail(null);
        }
    }

    private static boolean selectIfPresent(ListView<Task> listView, List<Task> tasksForColumn, String id) {
        return tasksForColumn.stream()
                .filter(t -> id.equals(t.getId()))
                .findFirst()
                .map(t -> {
                    listView.getSelectionModel().select(t);
                    return true;
                })
                .orElse(false);
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
            detailHistoryList.getItems().setAll(
                    task.getHistory().stream()
                            .sorted(Comparator.comparing(HistoryEntry::getTimestamp).reversed())
                            .map(entry -> entry.getTimestamp().format(HISTORY_TIMESTAMP_FORMAT) + " - " + entry.getMessage())
                            .collect(Collectors.toList()));
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
