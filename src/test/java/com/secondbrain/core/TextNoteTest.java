package com.secondbrain.core;

import org.junit.jupiter.api.Test;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;

public class TextNoteTest {

    @Test
    void testExtractContent() {
        TextNote note = new TextNote();
        Date testDate = new Date();
        note.setCreatedDate(testDate);
        
        note.extractContent();
        
        String expectedContent = "Catatan ide yang dibuat pada: " + testDate;
        assertEquals(expectedContent, note.getContent());
        System.out.println("✅ [SUCCESS] Test ekstraksi konten text note berjalan dan success");
    }
}
