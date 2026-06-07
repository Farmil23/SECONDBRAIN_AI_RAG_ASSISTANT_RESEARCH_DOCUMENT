package com.secondbrain.core;

public class TextSegment implements Comparable<TextSegment> {
    private String content;
    private String filename;
    private int score;

    public TextSegment(String content, String filename) {
        this.content = content;
        this.filename = filename;
        this.score = 0;
    }

    public String getContent() {
        return content;
    }

    public String getFilename() {
        return filename;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    @Override
    public int compareTo(TextSegment other) {
        // Descending order (higher score first)
        return Integer.compare(other.score, this.score);
    }
}
