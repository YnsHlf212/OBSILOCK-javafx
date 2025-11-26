package com.coffrefort.client;

import com.coffrefort.client.controllers.LoginController;
import com.coffrefort.client.controllers.MainController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Mini client lourd JavaFX d'exemple pour le projet « Coffre‑fort numérique ».
 * Objectif pédagogique: fournir une base exécutable, simple à lire, sur laquelle
 * les étudiants peuvent s'appuyer pour intégrer de vrais appels REST.
 */
public class App extends Application {

    private final ApiClient apiClient = new ApiClient();
    private String userEmail; // Email de l'utilisateur connecté

    @Override
    public void start(Stage stage) {
        stage.setTitle("Coffre‑fort numérique — Mini client");

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/coffrefort/client/login.fxml"));
            // Controller factory pour injecter ApiClient et callback
            loader.setControllerFactory(type -> {
                if (type == LoginController.class) {
                    LoginController c = new LoginController();
                    c.setApiClient(apiClient);
                    c.setOnSuccess((email) -> {
                        // Stocker l'email de l'utilisateur
                        this.userEmail = email;
                        // Ouvre la fenêtre principale via FXML
                        openMainAndClose(stage);
                    });
                    return c;
                }
                try {
                    return type.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            Parent root = loader.load();
            stage.setScene(new Scene(root, 420, 320));
            stage.show();
        } catch (Exception e) {
            throw new RuntimeException("Impossible de charger login.fxml", e);
        }
    }

    private void openMainAndClose(Stage loginStage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/coffrefort/client/main.fxml"));
            loader.setControllerFactory(type -> {
                if (type == MainController.class) {
                    MainController c = new MainController();
                    c.setApiClient(apiClient);
                    c.setUserEmail(userEmail); // Injecter l'email
                    return c;
                }
                try {
                    return type.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            Parent root = loader.load();
            Stage mainStage = new Stage();
            mainStage.setTitle("Coffre‑fort — Espace personnel");
            mainStage.setScene(new Scene(root, 1024, 680));
            mainStage.show();
            // Fermer la fenêtre de login
            loginStage.close();
        } catch (Exception e) {
            throw new RuntimeException("Impossible de charger main.fxml", e);
        }
    }

    public static void main(String[] args) {
        launch();
    }
}