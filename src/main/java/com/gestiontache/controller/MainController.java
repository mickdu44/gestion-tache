package com.gestiontache.controller;

import com.gestiontache.model.Priority;
import com.gestiontache.model.Recurrence;
import com.gestiontache.model.SubTask;
import com.gestiontache.model.Task;
import com.gestiontache.model.TaskStatistics;
import com.gestiontache.repository.TaskRepository;
import com.gestiontache.service.TaskService;
import com.gestiontache.util.DescriptionFormatter;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
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
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class MainController {

    private static final DateTimeFormatter DAY_FORMAT =
            DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH);
    private static final String ALL_PRIORITIES = "Toutes priorites";

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
    private Label detailPlaceholder;
    @FXML
    private VBox detailContent;
    @FXML
    private Label detailTitleLabel;
    @FXML
    private Label detailDateLabel;
    @FXML
    private Label detailPriorityLabel;
    @FXML
    private Label detailRecurrenceLabel;
    @FXML
    private TextFlow detailDescriptionFlow;
    @FXML
    private Label detailSubtasksTitle;
    @FXML
    private VBox detailSubtasksBox;
    @FXML
    private Separator detailAttachmentsSeparator;
    @FXML
    private Label detailAttachmentsTitle;
    @FXML
    private VBox detailAttachmentsBox;

    private TaskService taskService;
    private LocalDate currentDate;

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

        taskListView.setCellFactory(list -> new TaskListCell(
                this::onToggleCompleted, this::onEditTask, this::onDeleteTask,
                isSearching(), isReorderEnabled(), this::onTasksReordered));

        taskListView.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldValue, newValue) -> showTaskDetail(newValue));

        searchField.textProperty().addListener((obs, oldValue, newValue) -> refresh());

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
        currentDate = currentDate.minusDays(1);
        searchField.clear();
        refresh();
    }

    @FXML
    private void onNextDay() {
        currentDate = currentDate.plusDays(1);
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
        openTaskDialog(null, defaultDate).ifPresent(task -> {
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

    private void onEditTask(Task task) {
        LocalDate previousDate = task.getDate();
        openTaskDialog(task, task.getDate()).ifPresent(updated -> {
            taskService.updateTask(updated, previousDate);
            refresh();
        });
    }

    private void onDeleteTask(Task task) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Supprimer la tache");
        alert.setHeaderText("Supprimer \"" + task.getTitle() + "\" ?");
        alert.setContentText("Cette action est definitive.");
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
     * priority filter applied (otherwise the list view holds a subset of the
     * day's tasks, and a drag-and-drop move could not be persisted meaningfully).
     */
    private boolean isReorderEnabled() {
        return !isSearching() && ALL_PRIORITIES.equals(priorityFilterCombo.getValue());
    }

    /**
     * Opens the add/edit dialog. When {@code existing} is null a new task is
     * created and returned on confirmation; otherwise the existing task is
     * mutated in place and returned.
     */
    private Optional<Task> openTaskDialog(Task existing, LocalDate defaultDate) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/gestiontache/task-dialog.fxml"));
            DialogPane pane = loader.load();
            TaskDialogController controller = loader.getController();
            controller.fill(
                    existing != null ? existing.getTitle() : null,
                    existing != null ? existing.getDescription() : null,
                    existing != null ? existing.getPriority() : Priority.MOYENNE,
                    existing != null ? existing.getRecurrence() : Recurrence.AUCUNE,
                    existing != null ? existing.getDate() : defaultDate,
                    existing != null ? existing.getSubtasks() : null,
                    existing != null ? existing.getAttachments() : null);

            Dialog<Task> dialog = new Dialog<>();
            dialog.setTitle(existing == null ? "Nouvelle tache" : "Modifier la tache");
            dialog.setDialogPane(pane);

            Button okButton = (Button) pane.lookupButton(ButtonType.OK);
            okButton.disableProperty().bind(controller.getTitleField().textProperty().isEmpty());

            dialog.setResultConverter(buttonType -> {
                if (buttonType != ButtonType.OK) {
                    return null;
                }
                if (existing == null) {
                    Task task = new Task(controller.getTitle(), controller.getDescription(), controller.getDate());
                    task.setPriority(controller.getPriority());
                    task.setRecurrence(controller.getRecurrence());
                    task.setSubtasks(controller.getSubtasks());
                    task.setAttachments(controller.getAttachments());
                    return task;
                }
                existing.setTitle(controller.getTitle());
                existing.setDescription(controller.getDescription());
                existing.setPriority(controller.getPriority());
                existing.setRecurrence(controller.getRecurrence());
                existing.setDate(controller.getDate());
                existing.setSubtasks(controller.getSubtasks());
                existing.setAttachments(controller.getAttachments());
                return existing;
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
        prevDayButton.setDisable(searching);
        nextDayButton.setDisable(searching);
        todayButton.setDisable(searching);
        reportButton.setDisable(searching);

        taskListView.setCellFactory(list -> new TaskListCell(
                this::onToggleCompleted, this::onEditTask, this::onDeleteTask,
                searching, isReorderEnabled(), this::onTasksReordered));

        List<Task> tasks;
        if (searching) {
            String keyword = searchField.getText();
            tasks = taskService.search(keyword);
        } else {
            tasks = taskService.getTasksForDate(currentDate);
        }

        String priorityFilter = priorityFilterCombo.getValue();
        if (priorityFilter != null && !ALL_PRIORITIES.equals(priorityFilter)) {
            tasks = tasks.stream()
                    .filter(t -> priorityFilter.equals(t.getPriority().toString()))
                    .collect(Collectors.toList());
        }

        if (searching) {
            String keyword = searchField.getText();
            dateLabel.setText("Resultats de recherche");
            statusLabel.setText("\"" + keyword.trim() + "\" trouve dans le titre ou la description");
            countLabel.setText(tasks.size() + " tache(s) trouvee(s)");
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

    /** Displays the given task's full detail in the right-hand panel, or a placeholder when null. */
    private void showTaskDetail(Task task) {
        boolean hasSelection = task != null;
        detailPlaceholder.setVisible(!hasSelection);
        detailPlaceholder.setManaged(!hasSelection);
        detailContent.setVisible(hasSelection);
        detailContent.setManaged(hasSelection);

        if (!hasSelection) {
            return;
        }

        detailTitleLabel.setText(task.getTitle());
        detailDateLabel.setText(capitalize(task.getDate().format(DAY_FORMAT)));
        detailPriorityLabel.setText(task.getPriority().toString());
        detailPriorityLabel.getStyleClass().removeIf(c -> c.startsWith("priority-") && !c.equals("priority-badge"));
        detailPriorityLabel.getStyleClass().add("priority-" + task.getPriority().name().toLowerCase(Locale.ROOT));
        boolean recurring = task.getRecurrence() != Recurrence.AUCUNE;
        detailRecurrenceLabel.setText("🔁 " + task.getRecurrence());
        detailRecurrenceLabel.setVisible(recurring);
        detailRecurrenceLabel.setManaged(recurring);
        String description = task.getDescription();
        if (description == null || description.isBlank()) {
            Text empty = new Text("(Aucune description)");
            empty.getStyleClass().add("detail-description-empty");
            detailDescriptionFlow.getChildren().setAll(empty);
        } else {
            detailDescriptionFlow.getChildren().setAll(DescriptionFormatter.toNodes(description));
        }

        showSubtasks(task);
        showAttachments(task);
    }

    private void showSubtasks(Task task) {
        List<SubTask> subtasks = task.getSubtasks();
        boolean hasSubtasks = !subtasks.isEmpty();
        detailSubtasksTitle.setVisible(hasSubtasks);
        detailSubtasksTitle.setManaged(hasSubtasks);
        detailSubtasksBox.setVisible(hasSubtasks);
        detailSubtasksBox.setManaged(hasSubtasks);
        if (!hasSubtasks) {
            detailSubtasksBox.getChildren().clear();
            return;
        }
        long done = subtasks.stream().filter(SubTask::isCompleted).count();
        detailSubtasksTitle.setText("Sous-taches (" + done + "/" + subtasks.size() + ")");

        List<javafx.scene.Node> rows = new ArrayList<>();
        for (SubTask subtask : subtasks) {
            CheckBox checkBox = new CheckBox(subtask.getTitle());
            checkBox.setSelected(subtask.isCompleted());
            checkBox.getStyleClass().add("subtask-row");
            checkBox.setOnAction(e -> onToggleSubtask(task, subtask, checkBox.isSelected()));
            rows.add(checkBox);
        }
        detailSubtasksBox.getChildren().setAll(rows);
    }

    private void onToggleSubtask(Task task, SubTask subtask, boolean completed) {
        subtask.setCompleted(completed);
        taskService.updateTask(task, task.getDate());
        refresh();
    }

    private void showAttachments(Task task) {
        List<String> attachments = task.getAttachments();
        boolean hasAttachments = !attachments.isEmpty();
        detailAttachmentsSeparator.setVisible(hasAttachments);
        detailAttachmentsSeparator.setManaged(hasAttachments);
        detailAttachmentsTitle.setVisible(hasAttachments);
        detailAttachmentsTitle.setManaged(hasAttachments);
        detailAttachmentsBox.setVisible(hasAttachments);
        detailAttachmentsBox.setManaged(hasAttachments);
        if (!hasAttachments) {
            detailAttachmentsBox.getChildren().clear();
            return;
        }
        List<javafx.scene.Node> rows = new ArrayList<>();
        for (String path : attachments) {
            Hyperlink link = new Hyperlink(new File(path).getName());
            link.getStyleClass().add("attachment-link");
            link.setOnAction(e -> openAttachment(path));
            rows.add(link);
        }
        detailAttachmentsBox.getChildren().setAll(rows);
    }

    private void openAttachment(String path) {
        try {
            Desktop.getDesktop().open(new File(path));
        } catch (Exception e) {
            showInfo("Impossible d'ouvrir le fichier : " + path);
        }
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Gestion des taches");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private static String capitalize(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
