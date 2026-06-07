package com.secondbrain.service;

import com.secondbrain.core.DocumentSource;
import com.secondbrain.persistence.DocumentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SecondBrainAgent {
    private String name = "Jarvis-Web";

    // Memory Banks per User
    private java.util.Map<Long, VectorDatabase> userMemoryBanks = new java.util.concurrent.ConcurrentHashMap<>();

    // -> KONSEP PBO: ABSTRACTION (Abstraksi). Agen kebal terhadap perubahan API! Ia memanggil antarmuka 'BrainDriver' yang kosong, bukan memanggil OpenAI secara langsung.
    private BrainDriver brain;
    private DocumentRepository documentRepository;

    private org.springframework.context.ApplicationContext context;

    @Autowired // MEMBAWA SEMUA PAKET DAN TERHUBUNG
    public SecondBrainAgent(BrainDriver brain, DocumentRepository documentRepository,
            org.springframework.context.ApplicationContext context) {
        this.brain = brain;
        this.documentRepository = documentRepository;
        this.context = context;

        loadMemoryFromDatabase();
    }

    private VectorDatabase getMemoryBankForUser(Long userId) {
        return userMemoryBanks.computeIfAbsent(userId, k -> context.getBean(VectorDatabase.class));
    }

    private void loadMemoryFromDatabase() {
        // -> FIX: Dulu pakai findAll() yang mengambil dokumen SEMUA user.
        //    Sekarang di-group per userId agar dokumen user A tidak bocor ke user B.
        System.out.println("[" + name + "] Loading memory from database (per-user isolated)...");
        java.util.List<com.secondbrain.persistence.DocumentEntity> allEntities = documentRepository.findAll();

        // Group entitas per userId terlebih dahulu
        java.util.Map<Long, java.util.List<com.secondbrain.persistence.DocumentEntity>> grouped =
                new java.util.HashMap<>();
        for (com.secondbrain.persistence.DocumentEntity e : allEntities) {
            grouped.computeIfAbsent(e.getUserId(), k -> new java.util.ArrayList<>()).add(e);
        }

        // Restore dokumen hanya ke memory bank pemiliknya
        for (java.util.Map.Entry<Long, java.util.List<com.secondbrain.persistence.DocumentEntity>> entry : grouped.entrySet()) {
            Long userId = entry.getKey();
            VectorDatabase userMemory = getMemoryBankForUser(userId);
            for (com.secondbrain.persistence.DocumentEntity entity : entry.getValue()) {
                try {
                    java.nio.file.Path filePath = java.nio.file.Paths.get(entity.getFilePath());
                    if (java.nio.file.Files.exists(filePath)) {
                        com.secondbrain.core.PDFDocument doc = new com.secondbrain.core.PDFDocument();
                        doc.setFilePath(filePath);
                        doc.setDetails(entity.getFilename(), entity.getFilename(), userId);
                        doc.extractContent();
                        userMemory.save(doc);
                        System.out.println("[" + name + "] Restored for User " + userId + ": " + entity.getFilename());
                    } else {
                        System.err.println("[" + name + "] File not found (skipping): " + entity.getFilePath());
                    }
                } catch (Exception e) {
                    System.err.println("[" + name + "] Failed to restore: " + entity.getFilename());
                }
            }
        }
    }

    // -> KONSEP PBO: POLYMORPHISM OBJECT. Parameter siap sedia menangkap wujud abstrak 'DocumentSource'. Yang nanti datang masuk bisa 'PDFDocument', 'WordDocument', dll.
    public void learn(DocumentSource doc) {
        // -> INFO: Method ini terhubung pada 'VectorDatabase.java' untuk menyimpan teks ke RAM dan 'DocumentRepository.java' untuk menyimpan metadata ke MySQL
        // 1. Process and Save to RAM (VectorDB)
        doc.extractContent();
        VectorDatabase userMemory = getMemoryBankForUser(doc.getUserId());
        userMemory.save(doc);

        if (doc instanceof com.secondbrain.core.PDFDocument) {
            com.secondbrain.core.PDFDocument pdfDoc = (com.secondbrain.core.PDFDocument) doc;

            // Check if already exists to avoid duplicates
            if (documentRepository.findByFilename(pdfDoc.getFilename()).isEmpty()) {
                com.secondbrain.persistence.DocumentEntity entity = new com.secondbrain.persistence.DocumentEntity(
                        pdfDoc.getFilename(),
                        pdfDoc.getFilePath().toString(),
                        java.time.LocalDateTime.now(),
                        doc.getUserId());
                documentRepository.save(entity);
                System.out.println("[" + name + "] Saved metadata to Database for User " + doc.getUserId());
            }
        }

        String preview = doc.getContent().length() > 50 ? doc.getContent().substring(0, 50) : doc.getContent();
        System.out.println("[" + name + "] Selesai membaca data: " + preview + "...");
    }

    public String ask(String question, Long userId) {
        // -> INFO: Versi lama — search seluruh VectorDB user (tanpa filter dokumen)
        System.out.println(name + " sedang menghubungkan ke otak User " + userId + "...");
        VectorDatabase userMemory = getMemoryBankForUser(userId);
        String context = userMemory.search(question);
        String response = brain.think(context, question);
        System.out.println("Jawaban: " + response);
        return response;
    }

    /**
     * Versi baru: hanya gunakan dokumen yang ada di assignedDocIds sebagai konteks RAG.
     * Jika assignedDocIds kosong, fallback ke seluruh VectorDB user (perilaku lama).
     */
    public String askWithContext(String question, Long userId, java.util.List<Long> assignedDocIds) {
        System.out.println(name + " [context-aware] User=" + userId + " docs=" + assignedDocIds);
        VectorDatabase userMemory = getMemoryBankForUser(userId);

        String context;
        if (assignedDocIds == null || assignedDocIds.isEmpty()) {
            // Tidak ada doc yang di-assign — pakai semua konteks milik user
            context = userMemory.search(question);
        } else {
            // Ambil langsung dari DB berdasarkan ID (mengakomodasi dokumen grup dari user lain)
            java.util.List<com.secondbrain.persistence.DocumentEntity> docs = documentRepository.findAllById(assignedDocIds);
            if (docs.isEmpty()) {
                context = userMemory.search(question);
            } else {
                VectorDatabase miniMemory = this.context.getBean(VectorDatabase.class);
                for (com.secondbrain.persistence.DocumentEntity entity : docs) {
                    try {
                        java.nio.file.Path filePath = java.nio.file.Paths.get(entity.getFilePath());
                        if (java.nio.file.Files.exists(filePath)) {
                            com.secondbrain.core.PDFDocument doc = new com.secondbrain.core.PDFDocument();
                            doc.setFilePath(filePath);
                            doc.setDetails(entity.getFilename(), entity.getFilename(), entity.getUserId());
                            doc.extractContent();
                            miniMemory.save(doc);
                        }
                    } catch (Exception e) {
                        System.err.println("[" + name + "] Failed to load assigned doc context: " + entity.getFilename());
                    }
                }
                context = miniMemory.search(question);
            }
        }

        String response = brain.think(context, question);
        System.out.println("[context-aware] Jawaban: " + response);
        return response;
    }

    public int getMemorySize(Long userId) {
        return getMemoryBankForUser(userId).getDocumentCount();
    }

    public java.util.List<DocumentSource> getDocuments(Long userId) {
        return getMemoryBankForUser(userId).getDocuments();
    }

    /** Kembalikan DocumentEntity dari DB untuk ditampilkan di Document Panel (dengan ID). */
    public java.util.List<com.secondbrain.persistence.DocumentEntity> getDocumentEntities(Long userId) {
        return documentRepository.findAllByUserId(userId);
    }
}
