package com.secondbrain.persistence;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class ChatMessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private ChatSessionEntity chatSession;

    private String senderName;
    
    @Column(columnDefinition = "TEXT")
    private String messageContent;

    private LocalDateTime createdAt = LocalDateTime.now();

    public ChatMessageEntity() {}

    public ChatMessageEntity(ChatSessionEntity chatSession, String senderName, String messageContent) {
        this.chatSession = chatSession;
        this.senderName = senderName;
        this.messageContent = messageContent;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public ChatSessionEntity getChatSession() {
        return chatSession;
    }

    public void setChatSession(ChatSessionEntity chatSession) {
        this.chatSession = chatSession;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getMessageContent() {
        return messageContent;
    }

    public void setMessageContent(String messageContent) {
        this.messageContent = messageContent;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
