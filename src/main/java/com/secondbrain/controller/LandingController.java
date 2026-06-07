package com.secondbrain.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class LandingController {

    @Autowired
    private ApplicationContext applicationContext;

    /** "Get Started →" — arahkan ke halaman Register */
    @FXML
    public void handleGetStarted() {
        navigateTo("/com/secondbrain/login-view.fxml", "Second Brain — Login / Register", 900, 600);
    }

    /** "Already have an account? Login" — arahkan ke Login */
    @FXML
    public void handleGoToLogin() {
        navigateTo("/com/secondbrain/login-view.fxml", "Second Brain — Login", 900, 600);
    }

    private void navigateTo(String fxmlPath, String title, int w, int h) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();

            // Ambil stage dari node mana saja yang sudah di-render
            Stage stage = (Stage) javafx.stage.Window.getWindows()
                    .stream().filter(javafx.stage.Window::isShowing)
                    .filter(w2 -> w2 instanceof Stage)
                    .findFirst().orElseThrow();

            stage.setScene(new Scene(root, w, h));
            stage.setTitle(title);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
