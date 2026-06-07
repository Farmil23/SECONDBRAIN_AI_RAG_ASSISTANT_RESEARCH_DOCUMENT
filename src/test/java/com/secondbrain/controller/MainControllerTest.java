package com.secondbrain.controller;

import com.secondbrain.persistence.UserEntity;
import com.secondbrain.service.ChatService;
import com.secondbrain.service.SecondBrainAgent;
import com.secondbrain.service.UserService;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.ArrayList;

@ExtendWith(ApplicationExtension.class)
public class MainControllerTest {

    private SecondBrainAgent agent;
    private UserService userService;
    private ChatService chatService;
    private ApplicationContext applicationContext;
    
    private MainController mainController;

    @Start
    private void start(Stage stage) throws Exception {
        agent = Mockito.mock(SecondBrainAgent.class);
        userService = Mockito.mock(UserService.class);
        chatService = Mockito.mock(ChatService.class);
        applicationContext = Mockito.mock(ApplicationContext.class);

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/secondbrain/main-view.fxml"));
        
        fxmlLoader.setControllerFactory(clazz -> {
            if (clazz == MainController.class) {
                MainController controller = new MainController();
                try {
                    setField(controller, "agent", agent);
                    setField(controller, "userService", userService);
                    setField(controller, "chatService", chatService);
                    setField(controller, "applicationContext", applicationContext);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return controller;
            }
            try {
                return clazz.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        Parent root = fxmlLoader.load();
        mainController = fxmlLoader.getController();

        UserEntity testUser = new UserEntity("testuser", "pass", "FREE", 10);
        when(chatService.getUserSessions(testUser)).thenReturn(new ArrayList<>());
        
        Platform.runLater(() -> mainController.setCurrentUser(testUser));

        stage.setScene(new Scene(root, 900, 600));
        stage.show();
    }

    private void setField(Object obj, String fieldName, Object value) throws Exception {
        java.lang.reflect.Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(obj, value);
    }

    @Test
    void testMainControllerInitialization(FxRobot robot) {
        robot.interact(() -> {
            Label statusLabel = robot.lookup("#statusLabel").queryAs(Label.class);
            assertNotNull(statusLabel);
            assertTrue(statusLabel.getText().contains("testuser"));
            System.out.println("✅ [SUCCESS] Test inisialisasi MainController berjalan dan success");
        });
    }

    @Test
    void testSwitchMode(FxRobot robot) {
        robot.interact(() -> {
            try {
                java.lang.reflect.Method m = MainController.class.getDeclaredMethod("handleSwitchToGroup");
                m.setAccessible(true);
                m.invoke(mainController);
            } catch (Exception e) { }
        });
        
        robot.interact(() -> {
            Label badge = robot.lookup("#modeBadgeLabel").queryAs(Label.class);
            assertTrue(badge.getText().contains("GROUP"));
        });
        
        robot.interact(() -> {
            try {
                java.lang.reflect.Method m = MainController.class.getDeclaredMethod("handleSwitchToWorkspace");
                m.setAccessible(true);
                m.invoke(mainController);
            } catch (Exception e) { }
        });
        
        robot.interact(() -> {
            Label badge = robot.lookup("#modeBadgeLabel").queryAs(Label.class);
            assertTrue(badge.getText().contains("WORKSPACE"));
        });
        System.out.println("✅ [SUCCESS] Test button mode switch berjalan dan success");
    }
}
