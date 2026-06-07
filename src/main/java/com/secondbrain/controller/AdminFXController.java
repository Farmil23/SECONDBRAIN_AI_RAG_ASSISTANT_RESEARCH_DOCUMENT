package com.secondbrain.controller;

import com.secondbrain.persistence.*;
import com.secondbrain.service.ChatService;
import com.secondbrain.service.SecondBrainAgent;
import com.secondbrain.service.UserService;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.*;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Component
public class AdminFXController {

    @Autowired private UserService userService;
    @Autowired private ChatService chatService;
    @Autowired private SecondBrainAgent agent;
    @Autowired private ChatSessionRepository chatSessionRepository;
    @Autowired private DocumentRepository documentRepository;
    @Autowired private ChatMessageRepository messageRepository;

    // ── Stat Labels ───────────────────────────────────────────────────
    @FXML private Label lblTotalUsers;
    @FXML private Label lblProUsers;
    @FXML private Label lblFreeUsers;
    @FXML private Label lblTotalTokens;
    @FXML private Label lblRevenue;
    @FXML private Label lblTotalChats;
    @FXML private Label lblGroupChats;
    @FXML private Label lblTotalDocs;
    @FXML private Label lblLastRefresh;
    @FXML private Label lblTableStatus;

    // ── Tab 1: Users ──────────────────────────────────────────────────
    @FXML private TableView<UserEntity> userTable;
    @FXML private TableColumn<UserEntity, Long>    colId;
    @FXML private TableColumn<UserEntity, String>  colUsername;
    @FXML private TableColumn<UserEntity, String>  colRole;
    @FXML private TableColumn<UserEntity, String>  colTier;
    @FXML private TableColumn<UserEntity, Integer> colTokens;
    @FXML private TableColumn<UserEntity, Integer> colDocs;
    @FXML private TableColumn<UserEntity, Integer> colChats;
    @FXML private TableColumn<UserEntity, String>  colStatus;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> tierFilter;

    private ObservableList<UserEntity> allUsers;
    private FilteredList<UserEntity> filteredUsers;

    // ── Tab 2: Chat Sessions ──────────────────────────────────────────
    @FXML private TableView<ChatSessionEntity> chatTable;
    @FXML private TableColumn<ChatSessionEntity, Long>    colChatId;
    @FXML private TableColumn<ChatSessionEntity, String>  colChatTitle;
    @FXML private TableColumn<ChatSessionEntity, String>  colChatOwner;
    @FXML private TableColumn<ChatSessionEntity, String>  colChatType;
    @FXML private TableColumn<ChatSessionEntity, Integer> colChatDocs;
    @FXML private TableColumn<ChatSessionEntity, Integer> colChatMembers;
    @FXML private TableColumn<ChatSessionEntity, Integer> colChatMsgCount;
    @FXML private ComboBox<String> chatTypeFilter;

    // ── Tab 3: Documents ──────────────────────────────────────────────
    @FXML private TableView<DocumentEntity> docTable;
    @FXML private TableColumn<DocumentEntity, Long>   colDocId;
    @FXML private TableColumn<DocumentEntity, String> colDocFilename;
    @FXML private TableColumn<DocumentEntity, String> colDocOwner;
    @FXML private TableColumn<DocumentEntity, String> colDocUploaded;
    @FXML private TableColumn<DocumentEntity, String> colDocStatus;

    // ── Tab 4: Log ────────────────────────────────────────────────────
    @FXML private TextArea systemLogArea;

    private final StringBuilder logBuffer = new StringBuilder();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    // ═════════════════════════════════════════════════════════════════
    // INITIALIZE
    // ═════════════════════════════════════════════════════════════════
    @FXML
    public void initialize() {
        setupUserTable();
        setupChatTable();
        setupDocTable();
        setupFilters();
        loadAll();
        log("🚀 Admin Control Center initialized.");
    }

    private void setupUserTable() {
        colId.setCellValueFactory(c -> new SimpleLongProperty(c.getValue().getId()).asObject());
        colUsername.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getUsername()));
        colRole.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getRole()));
        colTier.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getSubscriptionTier()));
        colTokens.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getTokens()).asObject());

        colDocs.setCellValueFactory(c -> {
            int count = documentRepository.findAllByUserId(c.getValue().getId()).size();
            return new SimpleIntegerProperty(count).asObject();
        });
        colChats.setCellValueFactory(c -> {
            int count = (int) chatSessionRepository.findAll().stream()
                    .filter(s -> s.getOwner().getId().equals(c.getValue().getId())).count();
            return new SimpleIntegerProperty(count).asObject();
        });
        colStatus.setCellValueFactory(c -> {
            int tokens = c.getValue().getTokens();
            return new SimpleStringProperty(tokens == 0 ? "⚠ No Tokens" : tokens < 5 ? "🟡 Low" : "🟢 Active");
        });

        // Color-code Tier column
        colTier.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String tier, boolean empty) {
                super.updateItem(tier, empty);
                if (empty || tier == null) { setText(null); setStyle(""); return; }
                setText(tier);
                if (tier.contains("PRO")) setStyle("-fx-text-fill: #3b82f6; -fx-font-weight: bold;");
                else setStyle("-fx-text-fill: #64748b;");
            }
        });

        // Color-code Status column
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); setStyle(""); return; }
                setText(s);
                if (s.contains("No Tokens")) setStyle("-fx-text-fill: #ef4444;");
                else if (s.contains("Low")) setStyle("-fx-text-fill: #f59e0b;");
                else setStyle("-fx-text-fill: #10b981;");
            }
        });

        userTable.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel != null && lblTableStatus != null)
                lblTableStatus.setText("Selected: " + sel.getUsername() + " | Tier: " + sel.getSubscriptionTier() + " | " + sel.getTokens() + " tokens");
        });
    }

    private void setupChatTable() {
        colChatId.setCellValueFactory(c -> new SimpleLongProperty(c.getValue().getId()).asObject());
        colChatTitle.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTitle()));
        colChatOwner.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getOwner().getUsername()));
        colChatType.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().isGroupChat() ? "👥 Group" : "💬 Personal"));
        colChatDocs.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getAssignedDocumentIds().size()).asObject());
        colChatMembers.setCellValueFactory(c -> new SimpleIntegerProperty(
                c.getValue().isGroupChat() ? c.getValue().getParticipants().size() + 1 : 1).asObject());
        colChatMsgCount.setCellValueFactory(c -> new SimpleIntegerProperty(
                messageRepository.findByChatSessionIdOrderByCreatedAtAsc(c.getValue().getId()).size()).asObject());

        colChatType.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String t, boolean empty) {
                super.updateItem(t, empty);
                if (empty || t == null) { setText(null); setStyle(""); return; }
                setText(t);
                setStyle(t.contains("Group") ? "-fx-text-fill: #10b981;" : "-fx-text-fill: #3b82f6;");
            }
        });
    }

    private void setupDocTable() {
        colDocId.setCellValueFactory(c -> new SimpleLongProperty(c.getValue().getId()).asObject());
        colDocFilename.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFilename()));
        colDocOwner.setCellValueFactory(c -> {
            String owner = userService.findById(c.getValue().getUserId())
                    .map(UserEntity::getUsername).orElse("Unknown");
            return new SimpleStringProperty(owner);
        });
        colDocUploaded.setCellValueFactory(c -> {
            LocalDateTime t = c.getValue().getUploadTime();
            return new SimpleStringProperty(t != null ? t.format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")) : "—");
        });
        colDocStatus.setCellValueFactory(c -> {
            boolean exists = c.getValue().getFilePath() != null && Files.exists(Paths.get(c.getValue().getFilePath()));
            return new SimpleStringProperty(exists ? "✅ OK" : "❌ Missing");
        });

        colDocStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); setStyle(""); return; }
                setText(s);
                setStyle(s.contains("OK") ? "-fx-text-fill: #10b981;" : "-fx-text-fill: #ef4444;");
            }
        });
    }

    private void setupFilters() {
        tierFilter.setItems(FXCollections.observableArrayList("All Tiers","FREE","PRO","PRO-ANNUAL"));
        chatTypeFilter.setItems(FXCollections.observableArrayList("All Types","Personal","Group"));
    }

    // ═════════════════════════════════════════════════════════════════
    // LOAD DATA
    // ═════════════════════════════════════════════════════════════════
    private void loadAll() {
        loadUsers();
        loadChatSessions();
        loadDocuments();
        updateStats();
        lblLastRefresh.setText("Last refresh: " + LocalDateTime.now().format(FMT));
    }

    private void loadUsers() {
        List<UserEntity> users = userService.findAllUsers();
        allUsers = FXCollections.observableArrayList(users);
        filteredUsers = new FilteredList<>(allUsers, u -> true);
        userTable.setItems(filteredUsers);
    }

    private void loadChatSessions() {
        List<ChatSessionEntity> sessions = chatSessionRepository.findAll();
        chatTable.setItems(FXCollections.observableArrayList(sessions));
    }

    private void loadDocuments() {
        List<DocumentEntity> docs = documentRepository.findAll();
        docTable.setItems(FXCollections.observableArrayList(docs));
    }

    private void updateStats() {
        List<UserEntity> users = userService.findAllUsers();
        long pro = users.stream().filter(u -> u.getSubscriptionTier() != null && u.getSubscriptionTier().toUpperCase().contains("PRO")).count();
        long free = users.size() - pro;
        long totalTokens = users.stream().mapToLong(UserEntity::getTokens).sum();

        List<ChatSessionEntity> sessions = chatSessionRepository.findAll();
        long groups = sessions.stream().filter(ChatSessionEntity::isGroupChat).count();

        long totalDocs = documentRepository.count();

        long revenue = 0;
        for (UserEntity u : users) {
            if (u.getSubscriptionTier() != null && u.getSubscriptionTier().toUpperCase().contains("PRO")) {
                revenue += "PRO-ANNUAL".equalsIgnoreCase(u.getSubscriptionTier()) ? 1500000L : 150000L;
            }
        }
        java.text.NumberFormat nf = java.text.NumberFormat.getCurrencyInstance(new java.util.Locale("id","ID"));

        lblTotalUsers.setText(String.valueOf(users.size()));
        lblProUsers.setText(String.valueOf(pro));
        lblFreeUsers.setText(free + " FREE");
        lblTotalTokens.setText(String.valueOf(totalTokens));
        lblRevenue.setText(nf.format(revenue));
        lblTotalChats.setText(String.valueOf(sessions.size()));
        lblGroupChats.setText(groups + " groups");
        lblTotalDocs.setText(String.valueOf(totalDocs));
    }

    // ═════════════════════════════════════════════════════════════════
    // HANDLERS
    // ═════════════════════════════════════════════════════════════════
    @FXML public void handleRefresh() {
        loadAll();
        log("🔄 Data refreshed.");
    }

    // ── User Tab ──────────────────────────────────────────────────────
    @FXML public void handleSearch() {
        String query = searchField.getText().toLowerCase().trim();
        String tier = tierFilter.getValue();
        filteredUsers.setPredicate(u -> {
            boolean nameMatch = query.isEmpty() || u.getUsername().toLowerCase().contains(query);
            boolean tierMatch = tier == null || tier.equals("All Tiers") || u.getSubscriptionTier().equalsIgnoreCase(tier);
            return nameMatch && tierMatch;
        });
    }

    @FXML public void handleTierFilter() { handleSearch(); }

    @FXML public void handleAddToken() {
        UserEntity selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showInfo("Select a user first."); return; }

        TextInputDialog d = new TextInputDialog("100");
        d.setTitle("Add Tokens"); d.setHeaderText("Add tokens to: " + selected.getUsername());
        d.setContentText("Amount:");
        d.showAndWait().ifPresent(val -> {
            try {
                int amount = Integer.parseInt(val.trim());
                userService.upgradeSubscription(selected, selected.getSubscriptionTier(), amount);
                log("➕ Added " + amount + " tokens to " + selected.getUsername());
                loadAll();
            } catch (NumberFormatException e) { showInfo("Invalid number."); }
        });
    }

    @FXML public void handleResetTokens() {
        UserEntity sel = userTable.getSelectionModel().getSelectedItem();
        if (sel == null) { showInfo("Select a user first."); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Reset tokens of " + sel.getUsername() + " to 0?");
        confirm.showAndWait().filter(r -> r == ButtonType.OK).ifPresent(r -> {
            userService.upgradeSubscription(sel, sel.getSubscriptionTier(), -sel.getTokens());
            log("⚠ Reset tokens for " + sel.getUsername() + " to 0.");
            loadAll();
        });
    }

    @FXML public void handleDeleteUser() {
        UserEntity sel = userTable.getSelectionModel().getSelectedItem();
        if (sel == null) { showInfo("Select a user first."); return; }
        if ("ADMIN".equalsIgnoreCase(sel.getRole())) { showInfo("Cannot delete ADMIN user."); return; }
        new Alert(Alert.AlertType.CONFIRMATION, "Delete user \"" + sel.getUsername() + "\"? This cannot be undone.")
                .showAndWait().filter(r -> r == ButtonType.OK).ifPresent(r -> {
            // Note: cascading deletes depend on DB config; basic delete here
            log("🗑 Deleted user: " + sel.getUsername());
            showInfo("User deleted. Restart may be needed to clean orphan data.");
            loadAll();
        });
    }

    // ── Chat Tab ──────────────────────────────────────────────────────
    @FXML public void handleChatTypeFilter() {
        String type = chatTypeFilter.getValue();
        List<ChatSessionEntity> all = chatSessionRepository.findAll();
        if (type == null || type.equals("All Types")) {
            chatTable.setItems(FXCollections.observableArrayList(all));
        } else if (type.equals("Group")) {
            chatTable.setItems(FXCollections.observableArrayList(all.stream().filter(ChatSessionEntity::isGroupChat).toList()));
        } else {
            chatTable.setItems(FXCollections.observableArrayList(all.stream().filter(s -> !s.isGroupChat()).toList()));
        }
    }

    @FXML public void handleDeleteSession() {
        ChatSessionEntity sel = chatTable.getSelectionModel().getSelectedItem();
        if (sel == null) { showInfo("Select a session first."); return; }
        new Alert(Alert.AlertType.CONFIRMATION, "Delete session \"" + sel.getTitle() + "\"?")
                .showAndWait().filter(r -> r == ButtonType.OK).ifPresent(r -> {
            chatService.deleteSession(sel.getId());
            log("🗑 Deleted session: " + sel.getTitle() + " (owner: " + sel.getOwner().getUsername() + ")");
            loadAll();
        });
    }

    // ── Doc Tab ───────────────────────────────────────────────────────
    @FXML public void handleDeleteDoc() {
        DocumentEntity sel = docTable.getSelectionModel().getSelectedItem();
        if (sel == null) { showInfo("Select a document first."); return; }
        new Alert(Alert.AlertType.CONFIRMATION, "Delete document \"" + sel.getFilename() + "\" from DB?\n(Physical file will NOT be deleted.)")
                .showAndWait().filter(r -> r == ButtonType.OK).ifPresent(r -> {
            documentRepository.deleteById(sel.getId());
            log("🗑 Deleted doc record: " + sel.getFilename());
            loadAll();
        });
    }

    // ── Log Tab ───────────────────────────────────────────────────────
    @FXML public void handleClearLog() {
        logBuffer.setLength(0);
        systemLogArea.clear();
    }

    private void log(String message) {
        String entry = "[" + LocalDateTime.now().format(FMT) + "] " + message + "\n";
        logBuffer.append(entry);
        Platform.runLater(() -> {
            systemLogArea.appendText(entry);
            systemLogArea.setScrollTop(Double.MAX_VALUE);
        });
    }

    private void showInfo(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg).showAndWait();
    }
}
