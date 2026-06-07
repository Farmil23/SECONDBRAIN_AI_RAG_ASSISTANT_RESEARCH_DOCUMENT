package com.secondbrain.core;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import java.nio.file.Path;

// -> KONSEP PBO: INHERITANCE (Pewarisan). File ini adalah keturunan dari kelas abstrak/induk DocumentSource.
public class PDFDocument extends DocumentSource {
    private Path filePath;

    public void setFilePath(String path) {
        this.filePath = Path.of(path);
    }

    public void setFilePath(Path path) {
        this.filePath = path;
    }

    public Path getFilePath() {
        return filePath;
    }

    // -> KONSEP PBO: POLYMORPHISM (Overriding). Menimpa perilaku method kosong dari kelas induknya secara spesifik untuk membaca file PDF.
    @Override
    public void extractContent() {
        // LangChain otomatis membaca PDF tanpa perlu coding manual yang panjang
        try {
            Document document = FileSystemDocumentLoader.loadDocument(filePath, new ApachePdfBoxDocumentParser());
            this.rawContent = document.text();
            System.out.println("Berhasil mengekstrak PDF secara otonom: " + filePath);
        } catch (Exception e) {
            this.rawContent = "Error reading PDF: " + e.getMessage();
        }
    }
}
