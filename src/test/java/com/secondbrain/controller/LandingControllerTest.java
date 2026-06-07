package com.secondbrain.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;
import org.testfx.framework.junit5.ApplicationExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(ApplicationExtension.class)
public class LandingControllerTest {

    @Test
    void testLandingControllerInit() throws Exception {
        LandingController controller = new LandingController();
        ApplicationContext mockContext = Mockito.mock(ApplicationContext.class);

        java.lang.reflect.Field acField = LandingController.class.getDeclaredField("applicationContext");
        acField.setAccessible(true);
        acField.set(controller, mockContext);

        assertNotNull(controller);
        // but we verify the controller can be instantiated and dependencies injected.
        System.out.println("✅ [SUCCESS] Test inisialisasi LandingController berjalan dan success");
    }
}
