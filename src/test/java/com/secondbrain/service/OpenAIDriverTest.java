package com.secondbrain.service;

import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class OpenAIDriverTest {

    private OpenAIDriver openAIDriver;
    private ChatLanguageModel mockChatModel;

    @BeforeEach
    void setUp() {
        // Instantiate with a dummy key
        openAIDriver = new OpenAIDriver("dummy-key");
        
        // Mock the internal chat model
        mockChatModel = Mockito.mock(ChatLanguageModel.class);
        ReflectionTestUtils.setField(openAIDriver, "chatModel", mockChatModel);
    }

    @Test
    void testThink_Success() {
        String expectedResponse = "Ini adalah jawaban AI.";
        when(mockChatModel.generate(anyString())).thenReturn(expectedResponse);

        String response = openAIDriver.think("Context data", "Halo AI");

        assertEquals(expectedResponse, response);
        System.out.println("✅ [SUCCESS] Test OpenAI think method berjalan dan success");
    }

    @Test
    void testThink_Exception() {
        when(mockChatModel.generate(anyString())).thenThrow(new RuntimeException("API Timeout"));

        String response = openAIDriver.think("Context data", "Halo AI");

        assertTrue(response.contains("Error from OpenAI"));
        assertTrue(response.contains("API Timeout"));
        System.out.println("✅ [SUCCESS] Test penanganan error OpenAI API berjalan dan success");
    }
}
