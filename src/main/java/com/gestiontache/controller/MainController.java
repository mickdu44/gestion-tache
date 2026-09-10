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
import javafx.geometry.Rectangle2D;
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
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Screen;

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
import java.util.stream.Stream;

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
    private SplitPane mainSplitPane;
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
    private VBox detailPane;
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
    private ViewMode viewMode = ViewMode.KANBAN;

    /** The task currently shown/edited in the right-hand panel, or null when none is selected. */
    private Task selectedTask;
    /** The selected task's date before an in-panel edit, so a date change can move it between days. */
    private LocalDate selectedTaskPreviousDate;
    /** Set while populating the detail panel, so programmatic field updates don't re-trigger a persist. */
    private boolean loadingDetail;
    /**
     * In Kanban mode, {@link #detailContent} is reparented into this popup
     * instead of staying inline in {@link #detailPane} (a Kanban card is too
     * narrow to show the full detail next to it). Created lazily, reused
     * across selections, non-modal so the board stays clickable.
     */
    private Dialog<Void> kanbanDetailDialog;
    /** Holds {@link #detailContent} inside {@link #kanbanDetailDialog}, so a task with many subtasks/attachments/history entries scrolls instead of growing the popup past the screen. */
    private ScrollPane kanbanDetailScrollPane;

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
                this::onToggleCompleted, this::onDeleteTask, this::onEditTask,
                isSearching(), isReorderEnabled(), this::onTasksReordered));

        // Kanban columns use a compact card (KanbanTaskCell) instead of
        // TaskListCell's wide row, which doesn't fit a narrow column.
        for (ListView<Task> kanbanList : List.of(kanbanTodoList, kanbanInProgressList, kanbanDoneList)) {
            kanbanList.setCellFactory(list -> new KanbanTaskCell(this::onToggleCompleted, this::onEditTask));
        }
        setUpKanbanColumnDropTarget(kanbanTodoList, TaskStatus.A_FAIRE);
        setUpKanbanColumnDropTarget(kanbanInProgressList, TaskStatus.EN_COURS);
        setUpKanbanColumnDropTarget(kanbanDoneList, TaskStatus.TERMINEE);

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
     * Makes a Kanban column accept a card dragged from any of the three
     * columns (including its own, as a no-op) and move it to
     * {@code targetStatus}. {@code KanbanTaskCell} is the drag source, and
     * puts the dragged task's id on the dragboard rather than its index,
     * since the source and target lists differ.
     */
    private void setUpKanbanColumnDropTarget(ListView<Task> listView, TaskStatus targetStatus) {
        listView.setOnDragOver(event -> {
            if (event.getGestureSource() != listView && event.getDragboard().hasString()) {
                event.acceptTransferModes(TransferMode.MOVE);
            }
            event.consume();
        });
        listView.setOnDragEntered(event -> {
            if (event.getDragboard().hasString()) {
                listView.getStyleClass().add("kanban-column-drag-over");
            }
        });
        listView.setOnDragExited(event -> listView.getStyleClass().remove("kanban-column-drag-over"));
        listView.setOnDragDropped(event -> {
            Dragboard dragboard = event.getDragboard();
            boolean success = false;
            if (dragboard.hasString()) {
                Task dragged = findKanbanTaskById(dragboard.getString());
                if (dragged != null) {
                    moveKanbanTaskToStatus(dragged, targetStatus);
                    success = true;
                }
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }

    private Task findKanbanTaskById(String id) {
        for (ListView<Task> list : List.of(kanbanTodoList, kanbanInProgressList, kanbanDoneList)) {
            for (Task task : list.getItems()) {
                if (id.equals(task.getId())) {
                    return task;
                }
            }
        }
        return null;
    }

    /** Persists the status change from a Kanban drag-and-drop move and logs it in the task's history, like any other status change. */
    private void moveKanbanTaskToStatus(Task task, TaskStatus targetStatus) {
        TaskStatus oldStatus = task.getStatus();
        if (oldStatus == targetStatus) {
            return;
        }
        task.setStatus(targetStatus);
        task.addHistoryEntry("Statut change : " + oldStatus + " -> " + targetStatus);
        taskService.updateTask(task, task.getDate());
        refresh();
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
                this::onToggleCompleted, this::onDeleteTask, this::onEditTask,
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

        // The detail panel shows nothing useful in Kanban mode (the popup
        // takes over), so it's removed from the SplitPane entirely rather
        // than just hidden, letting the board use the full window width.
        boolean detailPaneShown = mainSplitPane.getItems().contains(detailPane);
        if (kanbanView && detailPaneShown) {
            mainSplitPane.getItems().remove(detailPane);
        } else if (!kanbanView && !detailPaneShown) {
            mainSplitPane.getItems().add(detailPane);
        }

        if (kanbanView) {
            showKanbanBoard(tasks, previouslySelectedId);
        } else {
            taskListView.getItems().setAll(tasks);
            if (previouslySelectedId != null) {
                tasks.stream()
                        .filter(t -> previouslySelectedId.equals(t.getId()))
                        .findFirst()
                        .ifPresentOrElse(t -> {
                            taskListView.getSelectionModel().select(t);
                            showTaskDetail(t);
                        }, () -> showTaskDetail(null));
            } else {
                showTaskDetail(null);
            }
        }
    }

    /**
     * Opens (or refreshes) the detail panel/popup for {@code task}. This is
     * the only way editing opens now: clicking a row or card only selects
     * it, it no longer shows the detail on its own (see the "Modifier"
     * button in {@link TaskListCell} and {@link KanbanTaskCell}).
     */
    private void onEditTask(Task task) {
        showTaskDetail(task);
    }

    /**
     * Splits {@code tasks} (the current day's tasks, already filtered/sorted
     * like the flat list) into the three status columns. If a task's popup
     * is currently open (e.g. it was just dragged to another column, or one
     * of its fields was just edited), it is kept open and refreshed with
     * the task's current data. Otherwise the task is only reselected for
     * visual highlight: refreshing the board for an unrelated reason (an
     * unrelated checkbox toggle, another task added or deleted, a filter
     * change, ...) must never pop a closed popup back open (see
     * {@link #onEditTask}).
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

        Task previouslySelected = previouslySelectedId == null ? null
                : Stream.of(todo, inProgress, done)
                        .flatMap(List::stream)
                        .filter(t -> previouslySelectedId.equals(t.getId()))
                        .findFirst()
                        .orElse(null);
        if (previouslySelected != null) {
            selectIfPresent(kanbanTodoList, todo, previouslySelected);
            selectIfPresent(kanbanInProgressList, inProgress, previouslySelected);
            selectIfPresent(kanbanDoneList, done, previouslySelected);
            if (kanbanDetailDialog != null && kanbanDetailDialog.isShowing()) {
                showTaskDetail(previouslySelected);
            }
        } else if (kanbanDetailDialog != null && kanbanDetailDialog.isShowing()) {
            // The task whose popup was open just vanished from the board
            // (deleted, or filtered/searched out): close it instead of
            // leaving it showing stale data for a task that's gone.
            showTaskDetail(null);
        }
    }

    private static void selectIfPresent(ListView<Task> listView, List<Task> tasksForColumn, Task task) {
        if (tasksForColumn.contains(task)) {
            listView.getSelectionModel().select(task);
        }
    }

    /** Populates the editable right-hand panel with the given task's detail, or shows a placeholder when null. */
    private void showTaskDetail(Task task) {
        boolean hasSelection = task != null;
        boolean kanban = viewMode == ViewMode.KANBAN;

        if (!kanban) {
            ensureDetailContentInPane();
            detailPlaceholder.setVisible(!hasSelection);
            detailPlaceholder.setManaged(!hasSelection);
            detailContent.setVisible(hasSelection);
            detailContent.setManaged(hasSelection);
        } else if (!hasSelection && kanbanDetailDialog != null) {
            kanbanDetailDialog.hide();
        }

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

        if (kanban) {
            ensureDetailContentInDialog();
            detailContent.setVisible(true);
            detailContent.setManaged(true);
            kanbanDetailDialog.setTitle(task.getTitle());
            kanbanDetailDialog.show();
        }
    }

    /** Moves {@link #detailContent} back into the inline panel and hides the Kanban popup, if any. */
    private void ensureDetailContentInPane() {
        if (detailContent.getParent() != detailPane) {
            if (kanbanDetailDialog != null) {
                kanbanDetailScrollPane.setContent(null);
                kanbanDetailDialog.hide();
            }
            detailPane.getChildren().add(detailContent);
        }
    }

    /**
     * Moves {@link #detailContent} into the (lazily created) Kanban detail
     * popup. The content sits inside a scroll pane and the dialog's size is
     * clamped to the visible screen, so a task with many subtasks,
     * attachments or history entries scrolls within the popup instead of
     * growing it past the screen's edges.
     */
    private void ensureDetailContentInDialog() {
        if (kanbanDetailDialog == null) {
            kanbanDetailScrollPane = new ScrollPane();
            kanbanDetailScrollPane.setFitToWidth(true);
            kanbanDetailScrollPane.getStyleClass().add("kanban-detail-scroll");

            kanbanDetailDialog = new Dialog<>();
            kanbanDetailDialog.initModality(Modality.NONE);
            kanbanDetailDialog.setDialogPane(new DialogPane());
            kanbanDetailDialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            kanbanDetailDialog.getDialogPane().getStylesheets()
                    .add(getClass().getResource("/com/gestiontache/style.css").toExternalForm());
            kanbanDetailDialog.getDialogPane().setContent(kanbanDetailScrollPane);
            kanbanDetailDialog.setResizable(true);

            Rectangle2D visualBounds = Screen.getPrimary().getVisualBounds();
            double maxWidth = Math.max(320, visualBounds.getWidth() - 80);
            double maxHeight = Math.max(320, visualBounds.getHeight() - 80);
            kanbanDetailDialog.getDialogPane().setMaxWidth(maxWidth);
            kanbanDetailDialog.getDialogPane().setMaxHeight(maxHeight);
            kanbanDetailDialog.getDialogPane().setPrefSize(Math.min(420, maxWidth), Math.min(640, maxHeight));
        }
        if (detailContent.getParent() != kanbanDetailScrollPane) {
            detailPane.getChildren().remove(detailContent);
            kanbanDetailScrollPane.setContent(detailContent);
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
