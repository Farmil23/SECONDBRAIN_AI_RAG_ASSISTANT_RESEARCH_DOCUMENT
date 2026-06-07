package com.secondbrain.service;

import com.secondbrain.core.DocumentSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class VectorDatabaseTest {

    private VectorDatabase vectorDatabase;

    @BeforeEach
    void setUp() {
        vectorDatabase = new VectorDatabase();
    }

    @Test
    void testSaveAndGetDocuments() {
        DocumentSource dummyDoc = new DocumentSource() {
            @Override
            public void extractContent() {}

            @Override
            public String getContent() {
                return "Paragraph 1\n\nParagraph 2\n\nParagraph 3";
            }

            @Override
            public String getFilename() {
                return "test.txt";
            }

            @Override
            public Long getUserId() {
                return 1L;
            }
        };

        vectorDatabase.save(dummyDoc);
        assertEquals(1, vectorDatabase.getDocumentCount());
        assertEquals("test.txt", vectorDatabase.getDocuments().get(0).getFilename());
        System.out.println("✅ [SUCCESS] Test simpan dan ambil dokumen di VectorDB berjalan dan success");
    }

    @Test
    void testSearch_ReturnsSmartContext() {
        DocumentSource dummyDoc = new DocumentSource() {
            @Override
            public void extractContent() {}

            @Override
            public String getContent() {
                return "This is a detailed paragraph about artificial intelligence.\n\n" +
                       "This paragraph mentions java programming and Spring Boot.\n\n" +
                       "Another generic text line here.";
            }

            @Override
            public String getFilename() {
                return "ai_java.txt";
            }

            @Override
            public Long getUserId() {
                return 1L;
            }
        };

        vectorDatabase.save(dummyDoc);

        String context = vectorDatabase.search("java artificial");
        
        assertNotNull(context);
        assertTrue(context.contains("artificial intelligence"));
        assertTrue(context.contains("java programming"));
        System.out.println("✅ [SUCCESS] Test pencarian konteks di VectorDB berjalan dan success");
    }

    @Test
    void testSearch_EmptyQuery() {
        String result = vectorDatabase.search("   ");
        assertEquals("", result);
        System.out.println("✅ [SUCCESS] Test pencarian kosong di VectorDB berjalan dan success");
    }
}
