package com.secondbrain.service;

import com.secondbrain.persistence.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SecondBrainAgentTest {

    @Mock
    private BrainDriver brain;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private ApplicationContext context;

    @Mock
    private VectorDatabase vectorDatabase;

    private SecondBrainAgent agent;

    @BeforeEach
    void setUp() {
        // Return empty list on loadMemoryFromDatabase
        when(documentRepository.findAll()).thenReturn(new ArrayList<>());
        
        agent = new SecondBrainAgent(brain, documentRepository, context);
    }

    @Test
    void testAsk() {
        when(context.getBean(VectorDatabase.class)).thenReturn(vectorDatabase);
        when(vectorDatabase.search(anyString())).thenReturn("mocked context");
        when(brain.think(anyString(), anyString())).thenReturn("mocked answer");

        String response = agent.ask("test question", 1L);

        assertEquals("mocked answer", response);
        verify(vectorDatabase, times(1)).search("test question");
        verify(brain, times(1)).think("mocked context", "test question");
        System.out.println("✅ [SUCCESS] Test fungsi ask pada SecondBrainAgent berjalan dan success");
    }
}
