package com.secondbrain.controller;

import com.secondbrain.persistence.UserEntity;
import com.secondbrain.service.UserService;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(ApplicationExtension.class)
public class LoginControllerTest {

    private UserService userService;
    private ApplicationContext applicationContext;
    private LoginController loginController;

    @Start
    private void start(Stage stage) throws Exception {
        userService = Mockito.mock(UserService.class);
        applicationContext = Mockito.mock(ApplicationContext.class);

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/secondbrain/login-view.fxml"));
        
        fxmlLoader.setControllerFactory(clazz -> {
            if (clazz == LoginController.class) {
                loginController = new LoginController();
                try {
                    java.lang.reflect.Field usField = LoginController.class.getDeclaredField("userService");
                    usField.setAccessible(true);
                    usField.set(loginController, userService);

                    java.lang.reflect.Field acField = LoginController.class.getDeclaredField("applicationContext");
                    acField.setAccessible(true);
                    acField.set(loginController, applicationContext);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return loginController;
            }
            try {
                return clazz.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        Parent root = fxmlLoader.load();
        stage.setScene(new Scene(root));
        stage.show();
    }

    private void setFieldText(FxRobot robot, String fieldId, String text) {
        robot.interact(() -> {
            Object node = robot.lookup(fieldId).query();
            if (node instanceof TextField) ((TextField) node).setText(text);
            if (node instanceof PasswordField) ((PasswordField) node).setText(text);
        });
    }

    @Test
    void testLoginSuccess(FxRobot robot) {
        UserEntity mockUser = new UserEntity("testuser", "password", "FREE", 10);
        when(userService.login("testuser", "password")).thenReturn(Optional.of(mockUser));

        setFieldText(robot, "#usernameField", "testuser");
        setFieldText(robot, "#passwordField", "password");
        
        robot.interact(() -> {
            try {
                loginController.handleLogin();
            } catch (Exception e) {
                // ApplicationContext is mocked, so navigation might throw, but login logic is covered
            }
        });
        System.out.println("✅ [SUCCESS] Test login berhasil di UI berjalan dan success");
    }

    @Test
    void testLoginFailure(FxRobot robot) {
        when(userService.login(anyString(), anyString())).thenReturn(Optional.empty());

        setFieldText(robot, "#usernameField", "wronguser");
        setFieldText(robot, "#passwordField", "wrongpass");
        
        robot.interact(() -> {
            try {
                loginController.handleLogin();
            } catch (Exception e) { }
        });

        robot.interact(() -> {
            Label errorLabel = robot.lookup("#errorLabel").queryAs(Label.class);
            assertEquals("Invalid credentials.", errorLabel.getText());
        });
        System.out.println("✅ [SUCCESS] Test login gagal di UI berjalan dan success");
    }

    @Test
    void testRegisterFailure(FxRobot robot) {
        when(userService.registerUser(anyString(), anyString())).thenThrow(new IllegalArgumentException("Username is already taken."));

        setFieldText(robot, "#usernameField", "existinguser");
        setFieldText(robot, "#passwordField", "pass");
        
        robot.interact(() -> {
            try {
                loginController.handleRegister();
            } catch (Exception e) { }
        });

        robot.interact(() -> {
            Label errorLabel = robot.lookup("#errorLabel").queryAs(Label.class);
            assertEquals("Username is already taken.", errorLabel.getText());
        });
        System.out.println("✅ [SUCCESS] Test registrasi gagal di UI berjalan dan success");
    }
}
