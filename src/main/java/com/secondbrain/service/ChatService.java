package com.secondbrain.service;

import com.secondbrain.persistence.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ChatService {

    @Autowired
    private ChatSessionRepository sessionRepository;

    @Autowired
    private ChatMessageRepository messageRepository;

    @Autowired
    private SecondBrainAgent agent;
    
    @Autowired
    private UserService userService;

    @Autowired
    private DocumentRepository documentRepository;

    public ChatSessionEntity createSession(String title, boolean isGroup, UserEntity owner) {
        ChatSessionEntity session = new ChatSessionEntity(title, isGroup, owner);
        return sessionRepository.save(session);
    }

    public List<ChatSessionEntity> getUserSessions(UserEntity user) {
        return sessionRepository.findSessionsForUser(user.getId(), user);
    }
    
    public Optional<ChatSessionEntity> getSession(Long sessionId) {
        return sessionRepository.findById(sessionId);
    }

    public void renameSession(Long sessionId, String newTitle) {
        sessionRepository.findById(sessionId).ifPresent(session -> {
            session.setTitle(newTitle);
            sessionRepository.save(session);
        });
    }

    public void deleteSession(Long sessionId) {
        sessionRepository.deleteById(sessionId);
    }

    public void joinGroup(Long sessionId, UserEntity user) {
        sessionRepository.findById(sessionId).ifPresent(session -> {
            if (session.isGroupChat() && !session.getParticipants().contains(user) && !session.getOwner().getId().equals(user.getId())) {
                session.getParticipants().add(user);
                sessionRepository.save(session);
            }
        });
    }

    public void sendMessage(Long sessionId, UserEntity sender, String content) {
        sessionRepository.findById(sessionId).ifPresent(session -> {
            ChatMessageEntity msg = new ChatMessageEntity(session, sender.getUsername(), content);
            messageRepository.save(msg);

            // Periksa keberadaan keyword AI khusus "@badsfarmil"
            String lowerContent = content.toLowerCase();
            if (lowerContent.contains("@badsfarmil") || lowerContent.contains("@ badsfarmil")) {
                // potong prefix @badsfarmil untuk mendapat pertanyaan aslinya
                String promptText = content.replaceAll("(?i)@\\s*badsfarmil", "").trim();
                if(promptText.isEmpty()) promptText = "Hello!";
                
                // Pastikan user masih punya token, lalu kurangi
                if (userService.deductToken(sender)) {
                    try {
                        String answer = agent.askWithContext(promptText, sender.getId(), session.getAssignedDocumentIds());
                        ChatMessageEntity aiMsg = new ChatMessageEntity(session, "Jarvis", answer);
                        messageRepository.save(aiMsg);
                    } catch(Exception e) {
                        ChatMessageEntity errorMsg = new ChatMessageEntity(session, "System", "Error AI: " + e.getMessage());
                        messageRepository.save(errorMsg);
                    }
                } else {
                    ChatMessageEntity aiMsg = new ChatMessageEntity(session, "System", "Saldo token " + sender.getUsername() + " habis! Silahkan upgrade.");
                    messageRepository.save(aiMsg);
                }
            }
        });
    }

    public List<ChatMessageEntity> getMessages(Long sessionId) {
        return messageRepository.findByChatSessionIdOrderByCreatedAtAsc(sessionId);
    }

    public void saveChatMessage(Long sessionId, String senderName, String content) {
        // -> [INFO] Menyimpan pesan chat ke database MySQL secara manual
        sessionRepository.findById(sessionId).ifPresent(session -> {
            ChatMessageEntity msg = new ChatMessageEntity(session, senderName, content);
            messageRepository.save(msg);
        });
    }

    /** Assign sebuah dokumen ke chat session sebagai konteks AI. */
    public void assignDocumentToSession(Long sessionId, Long docId) {
        sessionRepository.findById(sessionId).ifPresent(session -> {
            session.addAssignedDocumentId(docId);
            sessionRepository.save(session);
        });
    }

    /** Hapus assignment dokumen dari chat session. */
    public void removeDocumentFromSession(Long sessionId, Long docId) {
        sessionRepository.findById(sessionId).ifPresent(session -> {
            session.removeAssignedDocumentId(docId);
            sessionRepository.save(session);
        });
    }

    /** Ambil list ID dokumen yang di-assign ke session ini. */
    public List<Long> getAssignedDocumentIds(Long sessionId) {
        return sessionRepository.findById(sessionId)
                .map(ChatSessionEntity::getAssignedDocumentIds)
                .orElse(new java.util.ArrayList<>());
    }

    /** Ambil list map info dokumen lengkap dengan nama uploader. */
    public List<java.util.Map<String, Object>> getAssignedDocumentsWithUploader(Long sessionId, Long currentUserId) {
        List<java.util.Map<String, Object>> result = new java.util.ArrayList<>();
        sessionRepository.findById(sessionId).ifPresent(session -> {
            List<Long> assignedIds = session.getAssignedDocumentIds();
            if (!assignedIds.isEmpty()) {
                List<DocumentEntity> docs = documentRepository.findAllById(assignedIds);
                for (DocumentEntity doc : docs) {
                    java.util.Map<String, Object> map = new java.util.HashMap<>();
                    map.put("id", doc.getId());
                    map.put("filename", doc.getFilename());
                    map.put("isOwner", doc.getUserId().equals(currentUserId));
                    
                    String uploaderName = userService.findById(doc.getUserId())
                            .map(UserEntity::getUsername)
                            .orElse("Unknown");
                    map.put("uploaderName", uploaderName);
                    
                    result.add(map);
                }
            }
        });
        return result;
    }
}
