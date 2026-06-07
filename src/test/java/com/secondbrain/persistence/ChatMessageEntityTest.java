package com.secondbrain.persistence;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

public class ChatMessageEntityTest {

    @Test
    void testChatMessageEntityGettersSetters() {
        ChatSessionEntity session = new ChatSessionEntity();
        ChatMessageEntity msg = new ChatMessageEntity(session, "User1", "Hello AI");
        
        assertEquals(session, msg.getChatSession());
        assertEquals("User1", msg.getSenderName());
        assertEquals("Hello AI", msg.getMessageContent());
        assertNotNull(msg.getCreatedAt());
        
        ChatSessionEntity newSession = new ChatSessionEntity();
        msg.setChatSession(newSession);
        msg.setSenderName("User2");
        msg.setMessageContent("Hi");
        LocalDateTime newTime = LocalDateTime.now().plusDays(1);
        msg.setCreatedAt(newTime);
        
        assertEquals(newSession, msg.getChatSession());
        assertEquals("User2", msg.getSenderName());
        assertEquals("Hi", msg.getMessageContent());
        assertEquals(newTime, msg.getCreatedAt());
        assertNull(msg.getId()); // Not persisted
        System.out.println("✅ [SUCCESS] Test getter setter entitas pesan chat berjalan dan success");
    }

    @Test
    void testDefaultConstructor() {
        ChatMessageEntity msg = new ChatMessageEntity();
        assertNotNull(msg);
        assertNotNull(msg.getCreatedAt());
        System.out.println("✅ [SUCCESS] Test inisialisasi default entitas pesan chat berjalan dan success");
    }
}
