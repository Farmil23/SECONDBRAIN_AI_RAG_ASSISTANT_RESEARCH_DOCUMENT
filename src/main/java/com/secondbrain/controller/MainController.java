package com.secondbrain.controller;

import com.secondbrain.core.PDFDocument;
import com.secondbrain.persistence.*;
import com.secondbrain.service.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.*;

@Component
public class MainController {

    @Autowired private SecondBrainAgent agent;
    @Autowired private UserService userService;
    @Autowired private ChatService chatService;
    @Autowired private org.springframework.context.ApplicationContext applicationContext;

    private enum AppMode { WORKSPACE, GROUP }
    private AppMode currentMode = AppMode.WORKSPACE;
    private boolean docPanelOpen = false;

    // Sidebar
    @FXML private Label statusLabel;
    @FXML private Label tierLabel;
    @FXML private Label tokensLabel;
    @FXML private Button btnAdmin;
    @FXML private Button btnNavWorkspace;
    @FXML private Button btnNavGroup;
    @FXML private VBox workspaceSidePanel;
    @FXML private VBox groupSidePanel;
    @FXML private VBox chatSessionList;
    @FXML private VBox groupSessionList;

    // Chat header
    @FXML private HBox chatHeader;
    @FXML private Circle headerDot;
    @FXML private Label chatTitleLabel;
    @FXML private Label modeBadgeLabel;
    @FXML private Button btnToggleDocPanel;
    @FXML private HBox assignedDocsBar;
    @FXML private HBox assignedDocChips;

    // Chat area
    @FXML private ScrollPane chatScroll;
    @FXML private VBox chatHistoryBox;
    @FXML private TextField inputField;
    @FXML private Button sendButton;
    @FXML private HBox groupAiHintBar;
    @FXML private Button btnAskAI;

    // Doc panel
    @FXML private VBox docPanel;
    @FXML private VBox attachedDocList;
    @FXML private VBox allDocList;

    private UserEntity currentUser;
    private ChatSessionEntity currentSession;

    // ── Init ──────────────────────────────────────────────────────────
    public void setCurrentUser(UserEntity user) {
        this.currentUser = user;
        Platform.runLater(() -> {
            statusLabel.setText("Logged in as: " + user.getUsername());
            tierLabel.setText(user.getSubscriptionTier());
            tokensLabel.setText(user.getTokens() + " Tokens");
            btnAdmin.setVisible("ADMIN".equalsIgnoreCase(user.getRole()));
            btnAdmin.setManaged("ADMIN".equalsIgnoreCase(user.getRole()));
            chatHistoryBox.getChildren().clear();
            addChatBubble("Jarvis", "Welcome back, " + user.getUsername() + "! Select a chat or create one.", false);
        });
        refreshChatSessions();
    }

    // ── Mode Switching ────────────────────────────────────────────────
    @FXML public void handleSwitchToWorkspace() {
        if (currentMode == AppMode.WORKSPACE) return;
        currentMode = AppMode.WORKSPACE;
        currentSession = null;
        applyModeUI();
        clearAndWelcome("Workspace Mode - select a chat or create one.");
    }

    @FXML public void handleSwitchToGroup() {
        if (currentMode == AppMode.GROUP) return;
        currentMode = AppMode.GROUP;
        currentSession = null;
        closeDocPanel();
        applyModeUI();
        clearAndWelcome("Group Mode - select a group or create one. Mention @badsfarmil for AI.");
    }

    private void applyModeUI() {
        Platform.runLater(() -> {
            boolean ws = (currentMode == AppMode.WORKSPACE);
            workspaceSidePanel.setVisible(ws); workspaceSidePanel.setManaged(ws);
            groupSidePanel.setVisible(!ws); groupSidePanel.setManaged(!ws);
            btnToggleDocPanel.setVisible(true); btnToggleDocPanel.setManaged(true);

            // Tampilkan hint bar AI hanya di Group mode
            if (groupAiHintBar != null) {
                groupAiHintBar.setVisible(!ws); groupAiHintBar.setManaged(!ws);
            }

            btnNavWorkspace.getStyleClass().removeAll("nav-btn","nav-btn-workspace-active","nav-btn-group-active");
            btnNavGroup.getStyleClass().removeAll("nav-btn","nav-btn-workspace-active","nav-btn-group-active");
            btnNavWorkspace.getStyleClass().add(ws ? "nav-btn-workspace-active" : "nav-btn");
            btnNavGroup.getStyleClass().add(ws ? "nav-btn" : "nav-btn-group-active");

            headerDot.setFill(ws ? Color.web("#3b82f6") : Color.web("#10b981"));
            chatHeader.getStyleClass().removeAll("chat-header-workspace","chat-header-group");
            chatHeader.getStyleClass().add(ws ? "chat-header-workspace" : "chat-header-group");
            modeBadgeLabel.getStyleClass().removeAll("mode-badge-workspace","mode-badge-group");
            modeBadgeLabel.getStyleClass().add(ws ? "mode-badge-workspace" : "mode-badge-group");
            modeBadgeLabel.setText(ws ? "WORKSPACE" : "GROUP");
            inputField.setPromptText(ws ? "Ask about your documents..." : "Send a message...");
            sendButton.getStyleClass().removeAll("btn-primary","btn-group-primary");
            sendButton.getStyleClass().add(ws ? "btn-primary" : "btn-group-primary");
        });
    }

    private void clearAndWelcome(String msg) {
        Platform.runLater(() -> {
            chatHistoryBox.getChildren().clear();
            chatTitleLabel.setText("Select or create a chat");
            addChatBubble("Jarvis", msg, false);
            inputField.clear();
        });
    }

    // ── Document Panel ────────────────────────────────────────────────
    @FXML public void handleToggleDocPanel() {
        if (docPanelOpen) closeDocPanel();
        else openDocPanel();
    }

    /** Tombol "Ask AI →" di Group mode — auto-prepend @badsfarmil ke input field */
    @FXML public void handleAskAI() {
        Platform.runLater(() -> {
            String current = inputField.getText().trim();
            if (!current.toLowerCase().startsWith("@badsfarmil")) {
                inputField.setText("@badsfarmil " + current);
            }
            inputField.requestFocus();
            // Pindahkan kursor ke akhir teks
            inputField.end();
        });
    }

    private void openDocPanel() {
        docPanelOpen = true;
        docPanel.setVisible(true); docPanel.setManaged(true);
        btnToggleDocPanel.setText("📎 Docs ✕");
        refreshDocPanel();
    }

    private void closeDocPanel() {
        docPanelOpen = false;
        docPanel.setVisible(false); docPanel.setManaged(false);
        btnToggleDocPanel.setText("📎 Docs");
    }

    private void refreshDocPanel() {
        if (currentUser == null) return;
        Platform.runLater(() -> {
            List<DocumentEntity> allDocs = agent.getDocumentEntities(currentUser.getId());
            List<Long> assignedIds = currentSession != null
                    ? chatService.getAssignedDocumentIds(currentSession.getId())
                    : new ArrayList<>();

            // Attached list (docs assigned to this chat)
            attachedDocList.getChildren().clear();
            if (assignedIds.isEmpty()) {
                Label none = new Label("No documents attached yet.");
                none.setStyle("-fx-text-fill: #475569; -fx-font-size: 11px; -fx-padding: 4;");
                attachedDocList.getChildren().add(none);
            } else {
                if (currentSession != null) {
                    List<java.util.Map<String, Object>> groupDocs = chatService.getAssignedDocumentsWithUploader(currentSession.getId(), currentUser.getId());
                    for (java.util.Map<String, Object> map : groupDocs) {
                        attachedDocList.getChildren().add(buildSharedDocRow(map));
                    }
                }
            }

            // All docs list (not yet attached)
            allDocList.getChildren().clear();
            boolean hasUnattached = false;
            for (DocumentEntity doc : allDocs) {
                if (!assignedIds.contains(doc.getId())) {
                    allDocList.getChildren().add(buildDocRow(doc, false));
                    hasUnattached = true;
                }
            }
            if (!hasUnattached) {
                Label none = new Label("All your docs are attached.");
                none.setStyle("-fx-text-fill: #475569; -fx-font-size: 11px; -fx-padding: 4;");
                allDocList.getChildren().add(none);
            }

            // Update assigned docs bar in chat header
            refreshAssignedBar(assignedIds);
        });
    }

    private HBox buildSharedDocRow(java.util.Map<String, Object> map) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("doc-item");
        row.setMaxWidth(Double.MAX_VALUE);

        Long docId = (Long) map.get("id");
        String filename = (String) map.get("filename");
        String uploaderName = (String) map.get("uploaderName");
        boolean isOwner = (Boolean) map.get("isOwner");

        Circle dot = new Circle(4, Color.web("#10b981")); // Green for active doc
        
        VBox textBox = new VBox(2);
        Label name = new Label(filename);
        name.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 12px;");
        name.setMaxWidth(150); name.setEllipsisString("...");
        Label uploader = new Label("by: " + uploaderName);
        uploader.setStyle("-fx-text-fill: #64748b; -fx-font-size: 9px;");
        textBox.getChildren().addAll(name, uploader);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        Button action = new Button(isOwner ? "Detach" : "Locked");
        action.getStyleClass().add(isOwner ? "btn-icon-red" : "btn-icon");
        action.setStyle("-fx-font-size: 10px; -fx-padding: 2 8;");
        if (isOwner) {
            action.setOnAction(e -> { detachDoc(docId); refreshDocPanel(); });
        } else {
            action.setDisable(true);
        }

        row.getChildren().addAll(dot, textBox, action);
        return row;
    }

    private HBox buildDocRow(DocumentEntity doc, boolean isAttached) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("doc-item");
        row.setMaxWidth(Double.MAX_VALUE);

        Circle dot = new Circle(4, isAttached ? Color.web("#3b82f6") : Color.web("#475569"));
        Label name = new Label(doc.getFilename());
        name.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 12px;");
        name.setMaxWidth(170); name.setEllipsisString("...");
        HBox.setHgrow(name, Priority.ALWAYS);

        Button action = new Button(isAttached ? "Detach" : "Attach");
        action.getStyleClass().add(isAttached ? "btn-icon-red" : "btn-icon-blue");
        action.setStyle("-fx-font-size: 10px; -fx-padding: 2 8;");
        if (isAttached) {
            action.setOnAction(e -> { detachDoc(doc.getId()); refreshDocPanel(); });
        } else {
            action.setOnAction(e -> { attachDoc(doc.getId()); refreshDocPanel(); });
        }

        row.getChildren().addAll(dot, name, action);
        return row;
    }

    private void attachDoc(Long docId) {
        if (currentSession == null) {
            addChatBubble("System", "Please select or create a chat first before attaching a document.", false);
            return;
        }
        chatService.assignDocumentToSession(currentSession.getId(), docId);
        refreshChatSessions();
    }

    private void detachDoc(Long docId) {
        if (currentSession == null) return;
        chatService.removeDocumentFromSession(currentSession.getId(), docId);
        refreshChatSessions();
    }

    private void refreshAssignedBar(List<Long> assignedIds) {
        if (assignedIds.isEmpty() || currentSession == null) {
            assignedDocsBar.setVisible(false); assignedDocsBar.setManaged(false);
            return;
        }
        assignedDocChips.getChildren().clear();
        List<java.util.Map<String, Object>> groupDocs = chatService.getAssignedDocumentsWithUploader(currentSession.getId(), currentUser.getId());
        for (java.util.Map<String, Object> map : groupDocs) {
            String filename = (String) map.get("filename");
            Label chip = new Label("📄 " + filename);
            chip.setStyle("-fx-background-color: rgba(59,130,246,0.15); -fx-text-fill: #93c5fd;" +
                    "-fx-padding: 2 8; -fx-background-radius: 10; -fx-font-size: 10px;");
            assignedDocChips.getChildren().add(chip);
        }
        assignedDocsBar.setVisible(true); assignedDocsBar.setManaged(true);
    }

    @FXML public void handleNewChatWithDocs() {
        if (currentUser == null) return;
        List<Long> selected = new ArrayList<>();
        for (var node : allDocList.getChildren()) {
            if (node instanceof HBox row) {
                // collect all docs currently shown in allDocList
                for (var child : row.getChildren()) {
                    if (child instanceof Label l && l.getText().startsWith("📄")) {
                        // fallback: just create new chat then user attaches manually
                    }
                }
            }
        }
        TextInputDialog dialog = new TextInputDialog("New Chat");
        dialog.setTitle("New Chat with Docs");
        dialog.setHeaderText("Name for this chat:");
        dialog.showAndWait().ifPresent(name -> {
            ChatSessionEntity newSession = chatService.createSession(name, false, currentUser);
            // Attach all docs currently in attachedDocList to new session
            List<Long> currentAttached = currentSession != null
                    ? chatService.getAssignedDocumentIds(currentSession.getId()) : new ArrayList<>();
            for (Long id : currentAttached) {
                chatService.assignDocumentToSession(newSession.getId(), id);
            }
            currentSession = newSession;
            Platform.runLater(() -> {
                chatHistoryBox.getChildren().clear();
                chatTitleLabel.setText(name);
                addChatBubble("Jarvis", "Chat '" + name + "' created with your selected documents!", false);
                inputField.requestFocus();
                refreshChatSessions();
                refreshDocPanel();
            });
        });
    }

    // ── Send Message ──────────────────────────────────────────────────
    @FXML public void handleSend() {
        if (currentUser == null) {
            String q = inputField.getText().trim();
            if (q.isEmpty()) return;
            addChatBubble("You", q, true);
            addChatBubble("Jarvis", "Demo mode — please log in.", false);
            inputField.clear(); return;
        }
        String question = inputField.getText().trim();
        if (question.isEmpty()) return;
        if (currentSession == null) {
            addChatBubble("System", currentMode == AppMode.WORKSPACE
                    ? "Select or create a chat first (+ New)."
                    : "Select or create a group first (+ New).", false);
            inputField.clear(); return;
        }
        chatService.saveChatMessage(currentSession.getId(), currentUser.getUsername(), question);
        addChatBubble(currentUser.getUsername(), question, true);

        boolean isGroup = currentSession.isGroupChat();
        boolean triggerAI = !isGroup || question.toLowerCase().contains("@badsfarmil");
        if (triggerAI) {
            String q = question.replaceAll("(?i)@\\s*badsfarmil", "").trim();
            if (q.isEmpty()) q = "Hello!";
            final String fq = q;
            final List<Long> assignedIds = chatService.getAssignedDocumentIds(currentSession.getId());
            new Thread(() -> {
                if (userService.deductToken(currentUser)) {
                    Platform.runLater(() -> tokensLabel.setText(currentUser.getTokens() + " Tokens"));
                    String answer = assignedIds.isEmpty()
                            ? agent.ask(fq, currentUser.getId())
                            : agent.askWithContext(fq, currentUser.getId(), assignedIds);
                    chatService.saveChatMessage(currentSession.getId(), "Jarvis", answer);
                    addChatBubble("Jarvis", answer, false);
                } else {
                    addChatBubble("System", "Token habis! Silahkan upgrade.", false);
                }
            }).start();
        }
        inputField.clear();
    }

    // ── Chat bubble ───────────────────────────────────────────────────
    private void addChatBubble(String sender, String message, boolean isUser) {
        Platform.runLater(() -> {
            boolean isGroup = (currentMode == AppMode.GROUP);
            Color avatarColor = isUser ? Color.web("#334155")
                    : (isGroup ? Color.web("#10b981") : Color.web("#3b82f6"));
            Circle avatar = new Circle(11, avatarColor);
            Label nameLabel = new Label(sender);
            nameLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px; -fx-font-weight: bold;");
            HBox header = new HBox(6);
            header.setAlignment(isUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
            if (isUser) { header.getChildren().addAll(nameLabel, avatar); }
            else { header.getChildren().addAll(avatar, nameLabel); }
            Label msg = new Label(message);
            msg.setWrapText(true); msg.setMaxWidth(580);
            msg.getStyleClass().addAll("chat-bubble", isUser ? "chat-bubble-user" : "chat-bubble-bot");
            VBox box = new VBox(5);
            box.setAlignment(isUser ? Pos.TOP_RIGHT : Pos.TOP_LEFT);
            box.getChildren().addAll(header, msg);
            HBox container = new HBox(box);
            container.setAlignment(isUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
            container.setPadding(new Insets(6, 16, 6, 16));
            if (chatHistoryBox != null) {
                chatHistoryBox.getChildren().add(container);
                Platform.runLater(() -> { if (chatScroll != null) chatScroll.setVvalue(1.0); });
            }
        });
    }

    // ── Session CRUD ──────────────────────────────────────────────────
    @FXML public void handleNewChat() {
        if (currentUser == null) return;
        new TextInputDialog("New Chat").showAndWait().ifPresent(name -> {
            currentSession = chatService.createSession(name.isBlank() ? "New Chat" : name, false, currentUser);
            Platform.runLater(() -> {
                chatHistoryBox.getChildren().clear();
                chatTitleLabel.setText(currentSession.getTitle());
                addChatBubble("Jarvis", "Chat '" + currentSession.getTitle() + "' created. Attach docs via 📎 Docs!", false);
                inputField.requestFocus();
                refreshChatSessions();
                if (docPanelOpen) refreshDocPanel();
            });
        });
    }

    @FXML public void handleNewGroupChat() {
        if (currentUser == null) return;
        new TextInputDialog("New Group").showAndWait().ifPresent(name -> {
            currentSession = chatService.createSession(name.isBlank() ? "New Group" : name, true, currentUser);
            Platform.runLater(() -> {
                chatHistoryBox.getChildren().clear();
                chatTitleLabel.setText(currentSession.getTitle());
                addChatBubble("Jarvis", "Group '" + currentSession.getTitle() + "' created!", false);
                inputField.requestFocus();
                refreshChatSessions();
            });
        });
    }

    private void refreshChatSessions() {
        if (currentUser == null) return;
        Platform.runLater(() -> {
            List<ChatSessionEntity> sessions = chatService.getUserSessions(currentUser);
            if (chatSessionList != null) {
                chatSessionList.getChildren().clear();
                sessions.stream().filter(s -> !s.isGroupChat())
                        .forEach(s -> chatSessionList.getChildren().add(createSessionItem(s)));
            }
            if (groupSessionList != null) {
                groupSessionList.getChildren().clear();
                sessions.stream().filter(ChatSessionEntity::isGroupChat)
                        .forEach(s -> groupSessionList.getChildren().add(createSessionItem(s)));
            }
        });
    }

    private HBox createSessionItem(ChatSessionEntity session) {
        HBox item = new HBox(6);
        item.setAlignment(Pos.CENTER_LEFT);
        boolean active = currentSession != null && currentSession.getId().equals(session.getId());
        boolean isGroup = session.isGroupChat();
        item.getStyleClass().add(active
                ? (isGroup ? "session-item-active-group" : "session-item-active-workspace")
                : "session-item");

        Label icon = new Label(isGroup ? "👥" : "💬");
        icon.setStyle("-fx-font-size: 12px;");

        Label title = new Label(session.getTitle());
        title.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 12px;");
        title.setMaxWidth(90); title.setEllipsisString("...");
        HBox.setHgrow(title, Priority.ALWAYS);

        // Doc count badge
        List<Long> assignedIds = session.getAssignedDocumentIds();
        if (!assignedIds.isEmpty()) {
            Label badge = new Label(assignedIds.size() + " doc" + (assignedIds.size() > 1 ? "s" : ""));
            badge.setStyle("-fx-background-color: rgba(59,130,246,0.2); -fx-text-fill: #93c5fd;" +
                    "-fx-padding: 1 5; -fx-background-radius: 8; -fx-font-size: 9px;");
            item.getChildren().addAll(icon, title, badge);
        } else {
            item.getChildren().addAll(icon, title);
        }

        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        item.getChildren().add(spacer);

        Button btnRename = new Button("✎");
        btnRename.getStyleClass().add("btn-icon");
        btnRename.setOnAction(e -> {
            new TextInputDialog(session.getTitle()).showAndWait().ifPresent(t -> {
                chatService.renameSession(session.getId(), t);
                if (active) Platform.runLater(() -> chatTitleLabel.setText(t));
                refreshChatSessions();
            });
        });

        Button btnDelete = new Button("✕");
        btnDelete.getStyleClass().add("btn-icon-red");
        btnDelete.setOnAction(e -> {
            Alert a = new Alert(Alert.AlertType.CONFIRMATION, "Delete \"" + session.getTitle() + "\"?");
            a.showAndWait().filter(r -> r == ButtonType.OK).ifPresent(r -> {
                chatService.deleteSession(session.getId());
                if (active) { currentSession = null; Platform.runLater(() -> { chatHistoryBox.getChildren().clear(); chatTitleLabel.setText("Select a chat"); }); }
                refreshChatSessions();
            });
        });
        item.getChildren().addAll(btnRename, btnDelete);

        if (isGroup) {
            Button btnInvite = new Button("+");
            btnInvite.getStyleClass().add("btn-icon-green");
            btnInvite.setOnAction(e -> {
                new TextInputDialog().showAndWait().ifPresent(username ->
                        userService.findByUsername(username).ifPresentOrElse(
                                u -> { chatService.joinGroup(session.getId(), u); if (active) addChatBubble("System", u.getUsername() + " joined.", false); },
                                () -> new Alert(Alert.AlertType.ERROR, "User not found: " + username).showAndWait()
                        )
                );
            });
            item.getChildren().add(btnInvite);
        }

        item.setOnMouseClicked(e -> { if (e.getClickCount() == 1) loadSession(session); });
        return item;
    }

    private void loadSession(ChatSessionEntity session) {
        this.currentSession = session;
        Platform.runLater(() -> {
            chatHistoryBox.getChildren().clear();
            chatTitleLabel.setText(session.getTitle());
            List<ChatMessageEntity> msgs = chatService.getMessages(session.getId());
            if (msgs.isEmpty()) {
                addChatBubble("Jarvis", session.isGroupChat()
                        ? "Group '" + session.getTitle() + "' — mention @badsfarmil for AI."
                        : "Chat '" + session.getTitle() + "' — attach docs via 📎 then ask anything.", false);
            } else {
                for (ChatMessageEntity m : msgs)
                    addChatBubble(m.getSenderName(), m.getMessageContent(), m.getSenderName().equalsIgnoreCase(currentUser.getUsername()));
            }
            refreshChatSessions();
            if (docPanelOpen) refreshDocPanel();
            // Show assigned bar
            List<Long> ids = chatService.getAssignedDocumentIds(session.getId());
            refreshAssignedBar(ids);
        });
    }

    // ── Upload ────────────────────────────────────────────────────────
    @FXML public void handleUpload() {
        if (currentUser == null) return;
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        File f = fc.showOpenDialog(new Stage());
        if (f != null) {
            try {
                PDFDocument doc = new PDFDocument();
                doc.setFilePath(f.toPath());
                doc.setDetails("UPLOAD-" + System.currentTimeMillis(), f.getName(), currentUser.getId());
                agent.learn(doc);
                statusLabel.setText("✅ " + f.getName());
                if (docPanelOpen) refreshDocPanel();
            } catch (Exception e) { statusLabel.setText("❌ " + e.getMessage()); }
        }
    }

    // ── Upgrade / Admin / Logout ──────────────────────────────────────
    @FXML public void handleUpgrade() {
        if (currentUser == null) return;
        try {
            javafx.fxml.FXMLLoader l = new javafx.fxml.FXMLLoader(getClass().getResource("/com/secondbrain/upgrade-view.fxml"));
            l.setControllerFactory(applicationContext::getBean);
            javafx.scene.Parent root = l.load();
            UpgradeController uc = l.getController(); uc.initData(currentUser, this);
            Stage s = new Stage(); s.setScene(new javafx.scene.Scene(root));
            s.initModality(javafx.stage.Modality.APPLICATION_MODAL); s.setResizable(false); s.showAndWait();
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void onUpgradeSuccess(UserEntity u) {
        this.currentUser = u;
        Platform.runLater(() -> { tierLabel.setText(u.getSubscriptionTier()); tokensLabel.setText(u.getTokens() + " Tokens"); });
    }

    @FXML public void handleOpenAdmin() {
        if (currentUser == null || !"ADMIN".equalsIgnoreCase(currentUser.getRole())) return;
        try {
            javafx.fxml.FXMLLoader l = new javafx.fxml.FXMLLoader(getClass().getResource("/com/secondbrain/admin-view.fxml"));
            l.setControllerFactory(applicationContext::getBean);
            Stage s = new Stage(); s.setScene(new javafx.scene.Scene(l.load()));
            s.initModality(javafx.stage.Modality.APPLICATION_MODAL); s.showAndWait();
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML public void handleLogout() {
        try {
            javafx.fxml.FXMLLoader l = new javafx.fxml.FXMLLoader(getClass().getResource("/com/secondbrain/login-view.fxml"));
            l.setControllerFactory(applicationContext::getBean);
            Stage s = (Stage) statusLabel.getScene().getWindow();
            s.setScene(new javafx.scene.Scene(l.load(), 900, 600));
            s.setTitle("Second Brain — Login");
        } catch (Exception e) { e.printStackTrace(); }
    }
}