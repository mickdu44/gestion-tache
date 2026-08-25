package com.gestiontache.controller;

import com.gestiontache.model.Task;
import com.gestiontache.repository.TaskRepository;
import com.gestiontache.service.TaskService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class MainController {

    private static final DateTimeFormatter DAY_FORMAT =
            DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH);

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
    private Label detailStatusLabel;
    @FXML
    private Label detailDescriptionLabel;

    private TaskService taskService;
    private LocalDate currentDate;

    @FXML
    private void initialize() {
        taskService = new TaskService(new TaskRepository());
        currentDate = LocalDate.now();

        taskListView.setCellFactory(list -> new TaskListCell(
                this::onToggleCompleted, this::onEditTask, this::onDeleteTask, isSearching()));

        taskListView.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldValue, newValue) -> showTaskDetail(newValue));

        searchField.textProperty().addListener((obs, oldValue, newValue) -> refresh());

        refresh();
    }

    /** Called once after the window is shown, to offer reporting overdue tasks to today. */
    public void checkOverdueTasks() {
        List<Task> overdue = taskService.getOverdueUnfinishedTasks(LocalDate.now());
        if (overdue.isEmpty()) {
            return;
        }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Taches non terminees");
        alert.setHeaderText(overdue.size() + " tache(s) non terminee(s) provenant de jours precedents.");
        alert.setContentText("Voulez-vous les reporter a aujourd'hui ?");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            int moved = taskService.reportOverdueToToday(LocalDate.now());
            currentDate = LocalDate.now();
            searchField.clear();
            refresh();
            showInfo(moved + " tache(s) reportee(s) a aujourd'hui.");
        }
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

    private void onToggleCompleted(Task task, boolean completed) {
        taskService.setCompleted(task, completed);
        refresh();
    }

    private void onEditTask(Task task) {
        openTaskDialog(task, task.getDate()).ifPresent(updated -> {
            taskService.updateTask(updated);
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
                    existing != null ? existing.getDate() : defaultDate);

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
                    return new Task(controller.getTitle(), controller.getDescription(), controller.getDate());
                }
                existing.setTitle(controller.getTitle());
                existing.setDescription(controller.getDescription());
                existing.setDate(controller.getDate());
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
                this::onToggleCompleted, this::onEditTask, this::onDeleteTask, searching));

        List<Task> tasks;
        if (searching) {
            String keyword = searchField.getText();
            tasks = taskService.search(keyword);
            dateLabel.setText("Resultats de recherche");
            statusLabel.setText("\"" + keyword.trim() + "\" trouve dans le titre ou la description");
            countLabel.setText(tasks.size() + " tache(s) trouvee(s)");
        } else {
            tasks = taskService.getTasksForDate(currentDate);
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
        detailStatusLabel.setText(task.isCompleted() ? "Terminee" : "En cours");
        detailStatusLabel.getStyleClass().removeAll("status-done", "status-pending");
        detailStatusLabel.getStyleClass().add(task.isCompleted() ? "status-done" : "status-pending");
        String description = task.getDescription();
        detailDescriptionLabel.setText(
                description == null || description.isBlank() ? "(Aucune description)" : description);
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
