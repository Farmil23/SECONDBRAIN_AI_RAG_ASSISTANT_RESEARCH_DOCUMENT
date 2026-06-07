package com.secondbrain.controller;

import com.secondbrain.persistence.UserEntity;
import com.secondbrain.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.testfx.framework.junit5.ApplicationExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(ApplicationExtension.class)
public class UpgradeControllerTest {

    @Test
    void testUpgradeControllerInit() throws Exception {
        UpgradeController controller = new UpgradeController();
        UserService mockUserService = Mockito.mock(UserService.class);

        java.lang.reflect.Field usField = UpgradeController.class.getDeclaredField("userService");
        usField.setAccessible(true);
        usField.set(controller, mockUserService);

        UserEntity testUser = new UserEntity("test", "pass", "FREE", 10);
        MainController mockMain = Mockito.mock(MainController.class);
        
        // Ensure it doesn't crash on init
        controller.initData(testUser, mockMain);
        
        assertNotNull(controller);
        System.out.println("✅ [SUCCESS] Test inisialisasi UpgradeController berjalan dan success");
    }
}
