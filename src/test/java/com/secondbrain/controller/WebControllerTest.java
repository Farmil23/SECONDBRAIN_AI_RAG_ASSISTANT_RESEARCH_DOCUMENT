package com.secondbrain.controller;

import com.secondbrain.persistence.ChatSessionEntity;
import com.secondbrain.persistence.UserEntity;
import com.secondbrain.service.ChatService;
import com.secondbrain.service.SecondBrainAgent;
import com.secondbrain.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WebController.class)
public class WebControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SecondBrainAgent agent;

    @MockBean
    private UserService userService;

    @MockBean
    private ChatService chatService;

    private UserEntity mockUser;

    @BeforeEach
    void setUp() {
        mockUser = new UserEntity("testuser", "password", "FREE", 10);
        org.springframework.test.util.ReflectionTestUtils.setField(mockUser, "id", 1L);
    }

    @Test
    void testLoginPage() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
        System.out.println("✅ [SUCCESS] Test routing halaman login Web berjalan dan success");
    }

    @Test
    void testLoginSuccess() throws Exception {
        when(userService.login("testuser", "password")).thenReturn(Optional.of(mockUser));

        mockMvc.perform(post("/login")
                .param("username", "testuser")
                .param("password", "password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
        System.out.println("✅ [SUCCESS] Test proses login sukses via Web berjalan dan success");
    }

    @Test
    void testLoginFailure() throws Exception {
        when(userService.login(anyString(), anyString())).thenReturn(Optional.empty());

        mockMvc.perform(post("/login")
                .param("username", "wrong")
                .param("password", "wrong"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attributeExists("error"));
        System.out.println("✅ [SUCCESS] Test proses login gagal via Web berjalan dan success");
    }

    @Test
    void testRegisterSuccess() throws Exception {
        when(userService.registerUser(anyString(), anyString())).thenReturn(mockUser);

        mockMvc.perform(post("/register")
                .param("username", "newuser")
                .param("password", "pass"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attributeExists("message"));
        System.out.println("✅ [SUCCESS] Test proses registrasi via Web berjalan dan success");
    }

    @Test
    void testIndexPageRedirectsWhenNotLoggedIn() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
        System.out.println("✅ [SUCCESS] Test proteksi akses indeks Web berjalan dan success");
    }

    @Test
    void testIndexPageSuccess() throws Exception {
        when(userService.findById(mockUser.getId())).thenReturn(Optional.of(mockUser));
        when(chatService.getUserSessions(any(UserEntity.class))).thenReturn(new ArrayList<>());
        when(chatService.createSession(anyString(), any(Boolean.class), any(UserEntity.class)))
                .thenReturn(new ChatSessionEntity("General Room", true, mockUser));

        mockMvc.perform(get("/").sessionAttr("loggedInUser", mockUser))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));
        System.out.println("✅ [SUCCESS] Test akses indeks Web berjalan dan success");
    }

    @Test
    void testLogout() throws Exception {
        mockMvc.perform(get("/logout").sessionAttr("loggedInUser", mockUser))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
        System.out.println("✅ [SUCCESS] Test logout Web berjalan dan success");
    }
}
