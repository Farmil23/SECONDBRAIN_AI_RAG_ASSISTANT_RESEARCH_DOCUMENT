package com.secondbrain.service;

import com.secondbrain.core.DocumentSource;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

@Component
public class VectorDatabase {
    private List<com.secondbrain.core.TextSegment> memoryBank = new ArrayList<>();
    private List<DocumentSource> rawDocuments = new ArrayList<>(); // Keep raw for listing

    public void save(DocumentSource doc) {
        rawDocuments.add(doc);

        // Chunking Strategy: Split by paragraphs or roughly 1000 chars
        String fullText = doc.getContent();
        String[] paragraphs = fullText.split("\\n\\s*\\n"); // Split by empty lines

        for (String para : paragraphs) {
            if (para.trim().length() > 50) { // Ignore very short lines
                memoryBank.add(new com.secondbrain.core.TextSegment(para.trim(), doc.getFilename()));
            }
        }
    }

    public String search(String query) {
        if (query == null || query.trim().isEmpty())
            return "";

        // 1. Tokenize Query
        String[] keywords = query.toLowerCase().replaceAll("[^a-zA-Z0-9\\s]", "").split("\\s+");

        // 2. Score Chunks
        for (com.secondbrain.core.TextSegment segment : memoryBank) {
            int score = 0;
            String lowerContent = segment.getContent().toLowerCase();
            for (String word : keywords) {
                if (lowerContent.contains(word)) {
                    score++;
                }
            }
            segment.setScore(score);
        }

        // 3. Sort by Score
        java.util.Collections.sort(memoryBank);

        // 4. Select Top N Chunks (Smart Context)
        StringBuilder context = new StringBuilder();
        int tokenEstimate = 0;
        int maxTokens = 15000; // Safe limit for GPT-4o

        int count = 0;
        for (com.secondbrain.core.TextSegment segment : memoryBank) {
            if (segment.getScore() > 0 || count < 5) { // Always include top 5 even if score 0 (fallback)
                if (tokenEstimate + segment.getContent().length() > maxTokens)
                    break;

                context.append("\n--- Source: ").append(segment.getFilename()).append(" ---\n");
                context.append(segment.getContent()).append("\n");

                tokenEstimate += segment.getContent().length();
                count++;
            }
        }

        System.out.println("Smart Context Selected: " + count + " chunks out of " + memoryBank.size());
        return context.toString();
    }

    public int getDocumentCount() {
        return rawDocuments.size();
    }

    public List<DocumentSource> getDocuments() {
        return new ArrayList<>(rawDocuments);
    }
}
