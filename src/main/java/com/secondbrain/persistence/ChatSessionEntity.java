package com.secondbrain.persistence;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
public class ChatSessionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // increment
    private Long id;

    // Konsep PBO: Encapsulation
    private String title;
    
    private boolean isGroupChat;

    @ManyToOne
    @JoinColumn(name = "owner_id")
    
    private UserEntity owner;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "chat_session_participants",
        joinColumns = @JoinColumn(name = "chat_session_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private List<UserEntity> participants = new ArrayList<>();

    // Dokumen yang di-assign sebagai konteks AI untuk chat ini
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "chat_session_documents", joinColumns = @JoinColumn(name = "session_id"))
    @Column(name = "document_id")
    private List<Long> assignedDocumentIds = new ArrayList<>();

    public ChatSessionEntity() {}

    public ChatSessionEntity(String title, boolean isGroupChat, UserEntity owner) {
        this.title = title;
        this.isGroupChat = isGroupChat;
        this.owner = owner;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isGroupChat() {
        return isGroupChat;
    }

    public void setGroupChat(boolean groupChat) {
        isGroupChat = groupChat;
    }

    public UserEntity getOwner() {
        return owner;
    }

    public void setOwner(UserEntity owner) {
        this.owner = owner;
    }

    public List<UserEntity> getParticipants() {
        return participants;
    }

    public void setParticipants(List<UserEntity> participants) {
        this.participants = participants;
    }

    public List<Long> getAssignedDocumentIds() {
        return assignedDocumentIds;
    }

    public void setAssignedDocumentIds(List<Long> assignedDocumentIds) {
        this.assignedDocumentIds = assignedDocumentIds;
    }

    public void addAssignedDocumentId(Long docId) {
        if (!this.assignedDocumentIds.contains(docId)) {
            this.assignedDocumentIds.add(docId);
        }
    }

    public void removeAssignedDocumentId(Long docId) {
        this.assignedDocumentIds.remove(docId);
    }
}
