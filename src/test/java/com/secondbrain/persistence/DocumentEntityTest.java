package com.secondbrain.persistence;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

public class DocumentEntityTest {

    @Test
    void testDocumentEntityGettersSetters() {
        LocalDateTime now = LocalDateTime.now();
        DocumentEntity doc = new DocumentEntity("test.pdf", "/path/to/test.pdf", now, 1L);
        
        assertEquals("test.pdf", doc.getFilename());
        assertEquals("/path/to/test.pdf", doc.getFilePath());
        assertEquals(now, doc.getUploadTime());
        assertEquals(1L, doc.getUserId());
        
        doc.setFilename("new.pdf");
        doc.setFilePath("/new/path");
        LocalDateTime newTime = LocalDateTime.now().plusDays(1);
        doc.setUploadTime(newTime);
        doc.setUserId(2L);
        
        assertEquals("new.pdf", doc.getFilename());
        assertEquals("/new/path", doc.getFilePath());
        assertEquals(newTime, doc.getUploadTime());
        assertEquals(2L, doc.getUserId());
        assertNull(doc.getId()); // Not persisted
        System.out.println("✅ [SUCCESS] Test getter setter entitas dokumen berjalan dan success");
    }
    
    @Test
    void testDefaultConstructor() {
        DocumentEntity doc = new DocumentEntity();
        assertNotNull(doc);
        System.out.println("✅ [SUCCESS] Test inisialisasi default entitas dokumen berjalan dan success");
    }
}
