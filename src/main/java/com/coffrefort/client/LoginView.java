package com.coffrefort.client;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;

import java.util.function.BiConsumer;

/**
 * Vue de connexion minimaliste (sans FXML) destinée à être simple à lire par des débutants.
 */
public class LoginView {
    private final GridPane root = new GridPane();
    private final TextField emailField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    private final Label errorLabel = new Label();
    private BiConsumer<String, String> onLogin;

    public LoginView() {
        buildUi();
    }

    private void buildUi() {
        root.setPadding(new Insets(20));
        root.setHgap(10);
        root.setVgap(10);

        Text title = new Text("Connexion");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        root.add(title, 0, 0, 2, 1);
        root.add(new Label("Email"), 0, 1);
        root.add(emailField, 1, 1);
        root.add(new Label("Mot de passe"), 0, 2);
        root.add(passwordField, 1, 2);

        Button loginBtn = new Button("Se connecter");
        loginBtn.setDefaultButton(true);
        loginBtn.setOnAction(e -> triggerLogin());

        HBox actions = new HBox(10, loginBtn);
        actions.setAlignment(Pos.CENTER_RIGHT);
        root.add(actions, 1, 3);

        errorLabel.setStyle("-fx-text-fill: #b00020;");
        root.add(errorLabel, 1, 4);
    }

    private void triggerLogin() {
        if (onLogin != null) {
            showError("");
            onLogin.accept(emailField.getText(), passwordField.getText());
        }
    }

    public void setOnLogin(BiConsumer<String, String> onLogin) {
        this.onLogin = onLogin;
    }

    public void showError(String message) {
        errorLabel.setText(message == null ? "" : message);
    }

    public Node getRoot() {
        return root;
    }
}
