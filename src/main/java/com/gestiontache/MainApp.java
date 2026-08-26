package com.gestiontache;

import com.gestiontache.controller.MainController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/gestiontache/main.fxml"));
        Scene scene = new Scene(loader.load(), 1200, 720);

        MainController controller = loader.getController();

        stage.setTitle("Gestion des taches");
        stage.setScene(scene);
        stage.setMinWidth(720);
        stage.setMinHeight(480);
        try {
            stage.getIcons().add(new Image(Objects.requireNonNull(
                    getClass().getResourceAsStream("/com/gestiontache/icon.png"))));
        } catch (Exception ignored) {
            // Icon is purely cosmetic; the app still runs fine without it.
        }
        stage.show();

        // Runs once the window is visible so the "report overdue tasks" dialog
        // appears on top of an already-rendered window.
        Platform.runLater(controller::checkOverdueTasks);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
