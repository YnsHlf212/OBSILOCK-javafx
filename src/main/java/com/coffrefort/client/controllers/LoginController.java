package com.coffrefort.client.controllers;

import java.util.function.Consumer;

import com.coffrefort.client.ApiClient;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;

public class LoginController {
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Button loginButton;
    @FXML private ProgressIndicator loadingIndicator;

    private ApiClient apiClient;
    private Consumer<String> onSuccess; // Callback qui reçoit l'email

    public void setApiClient(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public void setOnSuccess(Consumer<String> onSuccess) {
        this.onSuccess = onSuccess;
    }

    @FXML
    public void initialize() {
        // Cacher l'indicateur de chargement au démarrage
        if (loadingIndicator != null) {
            loadingIndicator.setVisible(false);
        }
        
        // Valeurs par défaut pour les tests
        if (emailField != null) {
            emailField.setText("Yanis@test.fr");
        }
        if (passwordField != null) {
            passwordField.setText("yanistest123");
        }
    }

    @FXML
    public void handleLogin() {
        // Réinitialiser le message d'erreur
        if (errorLabel != null) {
            errorLabel.setText("");
        }
        
        String email = emailField != null ? emailField.getText().trim() : "";
        String password = passwordField != null ? passwordField.getText() : "";
        
        // Validation basique
        if (email.isEmpty() || password.isEmpty()) {
            if (errorLabel != null) {
                errorLabel.setText("Veuillez remplir tous les champs");
            }
            return;
        }
        
        // Désactiver le bouton pendant la connexion
        if (loginButton != null) {
            loginButton.setDisable(true);
            loginButton.setText("Connexion en cours...");
        }
        
        // Afficher l'indicateur de chargement
        if (loadingIndicator != null) {
            loadingIndicator.setVisible(true);
        }
        
        // Utiliser une Task pour exécuter l'appel API hors du thread JavaFX
        Task<Boolean> loginTask = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                // Appel à l'API de connexion
                return apiClient.login(email, password);
            }
        };
        
        // Gestion du succès
        loginTask.setOnSucceeded(event -> {
            Boolean success = loginTask.getValue();
            
            if (success != null && success) {
                // Connexion réussie - appeler le callback
                if (onSuccess != null) {
                    onSuccess.accept(email);
                }
            } else {
                // Connexion échouée (ne devrait pas arriver car une exception serait levée)
                showError("Échec de la connexion. Vérifiez vos identifiants.");
                resetLoginButton();
            }
        });
        
        // Gestion des erreurs
        loginTask.setOnFailed(event -> {
            Throwable exception = loginTask.getException();
            String errorMessage = "Erreur de connexion";
            
            if (exception != null) {
                String exMsg = exception.getMessage();
                
                // Messages d'erreur plus spécifiques selon le type d'erreur
                if (exMsg != null) {
                    if (exMsg.contains("401") || exMsg.contains("Échec de la connexion: 401")) {
                        errorMessage = "Email ou mot de passe incorrect";
                    } else if (exMsg.contains("ConnectException") || exMsg.contains("Connection refused")) {
                        errorMessage = "Impossible de contacter le serveur.\nVérifiez que le backend est démarré.";
                    } else if (exMsg.contains("UnknownHostException")) {
                        errorMessage = "Serveur introuvable. Vérifiez l'URL.";
                    } else if (exMsg.contains("timeout") || exMsg.contains("timed out")) {
                        errorMessage = "Délai d'attente dépassé.\nLe serveur ne répond pas.";
                    } else if (exMsg.contains("token manquant")) {
                        errorMessage = "Réponse invalide du serveur.\nToken manquant dans la réponse.";
                    } else {
                        errorMessage = "Erreur: " + exMsg;
                    }
                }
                
                // Log pour le débogage
                System.err.println("Erreur de connexion:");
                exception.printStackTrace();
            }
            
            showError(errorMessage);
            resetLoginButton();
        });
        
        // Démarrer la tâche dans un thread séparé
        Thread thread = new Thread(loginTask, "login-task-thread");
        thread.setDaemon(true);
        thread.start();
    }
    
    /**
     * Affiche un message d'erreur
     */
    private void showError(String message) {
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setStyle("-fx-text-fill: #f44336; -fx-font-weight: bold;");
        }
    }
    
    /**
     * Réinitialise le bouton de connexion
     */
    private void resetLoginButton() {
        if (loginButton != null) {
            loginButton.setDisable(false);
            loginButton.setText("Se connecter");
        }
        if (loadingIndicator != null) {
            loadingIndicator.setVisible(false);
        }
    }
}