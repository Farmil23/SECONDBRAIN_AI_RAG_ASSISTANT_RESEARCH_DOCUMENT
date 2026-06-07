package com.secondbrain.controller;

import com.secondbrain.persistence.UserEntity;
import com.secondbrain.service.UserService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;

@Component
public class LoginController {

    @Autowired
    private UserService userService;

    @Autowired
    private ApplicationContext applicationContext;

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    @FXML
    public void initialize() {
        // Run later to focus on the username field
        Platform.runLater(() -> usernameField.requestFocus());
    }

    @FXML
    public void handleLogin() {
        // -> INFO: Kode ini terhubung pada file 'UserService.java' method 'login()' untuk memvalidasi user dari database
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Username and Password are required.");
            return;
        }

        Optional<UserEntity> userOpt = userService.login(username, password);
        if (userOpt.isPresent()) {
            goToMainView(userOpt.get());
        } else {
            showError("Invalid credentials.");
        }
    }

    @FXML
    public void handleRegister() {
        // -> INFO: Kode ini terhubung pada file 'UserService.java' method 'registerUser()' untuk menyimpan data user baru
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Username and Password are required.");
            return;
        }

        try {
            UserEntity newUser = userService.registerUser(username, password);
            showSuccess("Registration successful! Please login.");
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        }
    }

    private void showError(String message) {
        errorLabel.setStyle("-fx-text-fill: #ef4444;");
        errorLabel.setText(message);
    }

    private void showSuccess(String message) {
        errorLabel.setStyle("-fx-text-fill: #10b981;");
        errorLabel.setText(message);
    }

    private void goToMainView(UserEntity user) {
        // -> INFO: Kode ini terhubung pada file 'main-view.fxml' untuk transisi antarmuka dan 'MainController.java' method 'setCurrentUser()' untuk passing data
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/secondbrain/main-view.fxml"));
            fxmlLoader.setControllerFactory(applicationContext::getBean); // Biar Spring yang inject Controller
            
            Parent root = fxmlLoader.load();

            // Set user di MainController
            MainController mainController = fxmlLoader.getController();
            mainController.setCurrentUser(user);

            // Ubah scene di Stage saat ini
            Stage stage = (Stage) usernameField.getScene().getWindow();
            Scene scene = new Scene(root, 900, 600);
            stage.setTitle("Second Brain Desktop AI - " + user.getUsername());
            stage.setScene(scene);
        } catch (IOException e) {
            showError("Failed to load main view.");
        }
    }
}
