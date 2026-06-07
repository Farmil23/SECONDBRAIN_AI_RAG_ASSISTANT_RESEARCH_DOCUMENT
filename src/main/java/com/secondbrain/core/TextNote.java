package com.secondbrain.core;

import java.util.Date;

public class TextNote extends DocumentSource {
    private Date createdDate;

    @Override
    public void extractContent() {
        this.rawContent = "Catatan ide yang dibuat pada: " + createdDate;
        System.out.println("Catatan teks berhasil diproses...");
    }

    public void setCreatedDate(Date date) {
        this.createdDate = date;
    }
}
