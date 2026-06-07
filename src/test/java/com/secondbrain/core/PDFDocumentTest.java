package com.secondbrain.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class PDFDocumentTest {

    @Test
    void testExtractContent_FileNotFound(@TempDir Path tempDir) {
        PDFDocument pdfDoc = new PDFDocument();
        pdfDoc.setFilePath(tempDir.resolve("nonexistent.pdf"));
        
        pdfDoc.extractContent();
        
        // It should handle the error and put the message in rawContent
        assertTrue(pdfDoc.getContent().contains("Error reading PDF"));
        System.out.println("✅ [SUCCESS] Test ekstraksi konten PDF yang tidak ada berjalan dan success");
    }

    // A real PDF test would require a dummy PDF file in test resources.
    // For now, we test the error handling path which validates LangChain integration tries to load.
}
