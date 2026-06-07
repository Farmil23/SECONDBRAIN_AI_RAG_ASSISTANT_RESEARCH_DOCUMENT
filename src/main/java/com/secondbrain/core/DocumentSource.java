package com.secondbrain.core;

public abstract class DocumentSource {
    private String id;
    private String filename;
    private Long userId; // Identifies owner
    protected String rawContent;

    public abstract void extractContent();

    public String getContent() {
        return rawContent; // Isi Dokumen
    }

    public void setDetails(String id, String filename, Long userId) {
        this.id = id; 
        this.filename = filename;
        this.userId = userId;
    }

    public String getFilename() {
        return filename;
    }

    public Long getUserId() {
        return userId;
    }
}
