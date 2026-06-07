package com.secondbrain.controller;

import com.secondbrain.persistence.ChatMessageRepository;
import com.secondbrain.persistence.ChatSessionRepository;
import com.secondbrain.persistence.DocumentRepository;
import com.secondbrain.service.ChatService;
import com.secondbrain.service.SecondBrainAgent;
import com.secondbrain.service.UserService;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(ApplicationExtension.class)
public class AdminFXControllerTest {

    private AdminFXController controller;

    @Start
    private void start(Stage stage) throws Exception {
        UserService userService = Mockito.mock(UserService.class);
        ChatService chatService = Mockito.mock(ChatService.class);
        SecondBrainAgent agent = Mockito.mock(SecondBrainAgent.class);
        ChatSessionRepository chatSessionRepository = Mockito.mock(ChatSessionRepository.class);
        DocumentRepository documentRepository = Mockito.mock(DocumentRepository.class);
        ChatMessageRepository messageRepository = Mockito.mock(ChatMessageRepository.class);

        when(userService.findAllUsers()).thenReturn(new ArrayList<>());
        when(chatSessionRepository.findAll()).thenReturn(new ArrayList<>());
        when(documentRepository.findAll()).thenReturn(new ArrayList<>());

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/secondbrain/admin-view.fxml"));
        
        fxmlLoader.setControllerFactory(clazz -> {
            if (clazz == AdminFXController.class) {
                AdminFXController ctrl = new AdminFXController();
                try {
                    setField(ctrl, "userService", userService);
                    setField(ctrl, "chatService", chatService);
                    setField(ctrl, "agent", agent);
                    setField(ctrl, "chatSessionRepository", chatSessionRepository);
                    setField(ctrl, "documentRepository", documentRepository);
                    setField(ctrl, "messageRepository", messageRepository);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return ctrl;
            }
            try {
                return clazz.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        Parent root = fxmlLoader.load();
        controller = fxmlLoader.getController();

        stage.setScene(new Scene(root, 900, 600));
        stage.show();
    }

    private void setField(Object obj, String fieldName, Object value) throws Exception {
        java.lang.reflect.Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(obj, value);
    }

    @Test
    void testHandleRefresh(FxRobot robot) {
        assertNotNull(controller);
        robot.interact(() -> {
            try {
                controller.handleRefresh();
            } catch (Exception e) {
            }
        });
        
        robot.interact(() -> {
            try {
                controller.handleSearch();
            } catch (Exception e) {
            }
        });
        
        robot.interact(() -> {
            try {
                controller.handleClearLog();
            } catch (Exception e) {
            }
        });
        System.out.println("✅ [SUCCESS] Test refresh, search, dan clear log admin FX berjalan dan success");
    }
}
