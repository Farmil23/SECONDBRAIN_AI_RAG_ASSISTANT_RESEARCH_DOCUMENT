package com.secondbrain.service;

import com.secondbrain.persistence.ChatMessageEntity;
import com.secondbrain.persistence.ChatMessageRepository;
import com.secondbrain.persistence.ChatSessionEntity;
import com.secondbrain.persistence.ChatSessionRepository;
import com.secondbrain.persistence.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ChatServiceTest {

    @Mock
    private ChatSessionRepository sessionRepository;

    @Mock
    private ChatMessageRepository messageRepository;

    @Mock
    private SecondBrainAgent agent;

    @Mock
    private UserService userService;

    @InjectMocks
    private ChatService chatService;

    private UserEntity mockUser;
    private ChatSessionEntity mockSession;

    @BeforeEach
    void setUp() {
        mockUser = new UserEntity("testuser", "pass", "FREE", 10);
        ReflectionTestUtils.setField(mockUser, "id", 1L);

        mockSession = new ChatSessionEntity("Test Session", false, mockUser);
        ReflectionTestUtils.setField(mockSession, "id", 10L);
    }

    @Test
    void testCreateSession() {
        when(sessionRepository.save(any(ChatSessionEntity.class))).thenAnswer(i -> i.getArguments()[0]);

        ChatSessionEntity result = chatService.createSession("New Session", false, mockUser);

        assertNotNull(result);
        assertEquals("New Session", result.getTitle());
        assertFalse(result.isGroupChat());
        assertEquals(mockUser, result.getOwner());
        verify(sessionRepository, times(1)).save(any(ChatSessionEntity.class));
        System.out.println("✅ [SUCCESS] Test pembuatan grup/sesi pada ini berjalan dan success");
    }

    @Test
    void testGetSession() {
        when(sessionRepository.findById(10L)).thenReturn(Optional.of(mockSession));

        Optional<ChatSessionEntity> result = chatService.getSession(10L);

        assertTrue(result.isPresent());
        assertEquals("Test Session", result.get().getTitle());
        System.out.println("✅ [SUCCESS] Test pengambilan sesi chat/grup berjalan dan success");
    }

    @Test
    void testSendMessage_Normal() {
        when(sessionRepository.findById(10L)).thenReturn(Optional.of(mockSession));

        chatService.sendMessage(10L, mockUser, "Hello world");

        verify(messageRepository, times(1)).save(any(ChatMessageEntity.class));
        verify(agent, never()).askWithContext(anyString(), anyLong(), anyList());
        System.out.println("✅ [SUCCESS] Test pengiriman pesan normal berjalan dan success");
    }

    @Test
    void testSendMessage_WithAI_SufficientTokens() throws Exception {
        when(sessionRepository.findById(10L)).thenReturn(Optional.of(mockSession));
        when(userService.deductToken(mockUser)).thenReturn(true);
        when(agent.askWithContext(anyString(), eq(mockUser.getId()), anyList())).thenReturn("Hello from AI");

        chatService.sendMessage(10L, mockUser, "@badsfarmil what is this?");

        // 1 user message + 1 AI response
        verify(messageRepository, times(2)).save(any(ChatMessageEntity.class));
        verify(agent, times(1)).askWithContext("what is this?", mockUser.getId(), mockSession.getAssignedDocumentIds());
        System.out.println("✅ [SUCCESS] Test pengiriman pesan ke AI dengan token cukup berjalan dan success");
    }

    @Test
    void testSendMessage_WithAI_InsufficientTokens() {
        when(sessionRepository.findById(10L)).thenReturn(Optional.of(mockSession));
        when(userService.deductToken(mockUser)).thenReturn(false);

        chatService.sendMessage(10L, mockUser, "@badsfarmil what is this?");

        // 1 user message + 1 system error response
        verify(messageRepository, times(2)).save(any(ChatMessageEntity.class));
        verify(agent, never()).askWithContext(anyString(), anyLong(), anyList());
        System.out.println("✅ [SUCCESS] Test pengiriman pesan ke AI saat token habis berjalan dan success");
    }

    @Test
    void testAssignDocumentToSession() {
        when(sessionRepository.findById(10L)).thenReturn(Optional.of(mockSession));

        chatService.assignDocumentToSession(10L, 99L);

        assertTrue(mockSession.getAssignedDocumentIds().contains(99L));
        verify(sessionRepository, times(1)).save(mockSession);
        System.out.println("✅ [SUCCESS] Test assign dokumen ke grup pada ini berjalan dan success");
    }
}
