package com.secondbrain.controller;

import com.secondbrain.persistence.UserEntity;
import com.secondbrain.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminController.class)
public class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    private UserEntity adminUser;
    private UserEntity normalUser;

    @BeforeEach
    void setUp() {
        adminUser = new UserEntity("admin", "pass", "PRO", 100);
        adminUser.setRole("ADMIN");
        org.springframework.test.util.ReflectionTestUtils.setField(adminUser, "id", 1L);

        normalUser = new UserEntity("normal", "pass", "FREE", 10);
        normalUser.setRole("USER");
        org.springframework.test.util.ReflectionTestUtils.setField(normalUser, "id", 2L);
    }

    @Test
    void testAdminDashboardNotLoggedIn() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
        System.out.println("✅ [SUCCESS] Test proteksi dashboard admin tanpa login berjalan dan success");
    }

    @Test
    void testAdminDashboardAsNormalUser() throws Exception {
        mockMvc.perform(get("/admin").sessionAttr("loggedInUser", normalUser))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
        System.out.println("✅ [SUCCESS] Test proteksi dashboard admin sebagai user biasa berjalan dan success");
    }

    @Test
    void testAdminDashboardAsAdmin() throws Exception {
        when(userService.findAllUsers()).thenReturn(Arrays.asList(adminUser, normalUser));

        mockMvc.perform(get("/admin").sessionAttr("loggedInUser", adminUser))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-dashboard"))
                .andExpect(model().attributeExists("users", "totalUsers", "totalTokens", "proUsers"));
        System.out.println("✅ [SUCCESS] Test akses dashboard admin oleh admin berjalan dan success");
    }

    @Test
    void testAddTokensAsAdmin() throws Exception {
        when(userService.findById(2L)).thenReturn(Optional.of(normalUser));

        mockMvc.perform(post("/admin/add-tokens")
                .sessionAttr("loggedInUser", adminUser)
                .param("userId", "2")
                .param("tokenAmount", "50"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"))
                .andExpect(flash().attributeExists("message"));
        System.out.println("✅ [SUCCESS] Test top up saldo oleh admin berjalan dan success");
    }

    @Test
    void testUpgradeProAsAdmin() throws Exception {
        when(userService.findById(2L)).thenReturn(Optional.of(normalUser));

        mockMvc.perform(post("/admin/upgrade-pro")
                .sessionAttr("loggedInUser", adminUser)
                .param("userId", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"))
                .andExpect(flash().attributeExists("message"));
        System.out.println("✅ [SUCCESS] Test upgrade pro oleh admin berjalan dan success");
    }
}
