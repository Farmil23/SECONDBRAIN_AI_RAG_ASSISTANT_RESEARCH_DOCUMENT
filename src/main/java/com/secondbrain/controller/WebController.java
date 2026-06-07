package com.secondbrain.controller;

import com.secondbrain.core.PDFDocument;
import com.secondbrain.persistence.UserEntity;
import com.secondbrain.service.SecondBrainAgent;
import com.secondbrain.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.secondbrain.persistence.ChatSessionEntity;
import com.secondbrain.service.ChatService;

@Controller
public class WebController {

    private final SecondBrainAgent agent;
    private final UserService userService;
    private final ChatService chatService;

    @Autowired
    public WebController(SecondBrainAgent agent, UserService userService, ChatService chatService) {
        this.agent = agent;
        this.userService = userService;
        this.chatService = chatService;
    }

    // Removed old getChatHistory memory method

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam("username") String username,
            @RequestParam("password") String password,
            HttpSession session, RedirectAttributes redirectAttributes) {
        // Kode ini terhubung pada file 'UserService.java' method 'login()' untuk autentikasi user di versi Web
        Optional<UserEntity> userOpt = userService.login(username, password);
        if (userOpt.isPresent()) {
            session.setAttribute("loggedInUser", userOpt.get());
            return "redirect:/";
        } else {
            redirectAttributes.addFlashAttribute("error", "Invalid username or password.");
            return "redirect:/login";
        }
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @PostMapping("/register")
    public String register(@RequestParam("username") String username,
            @RequestParam("password") String password,
            RedirectAttributes redirectAttributes) {
        // Kode ini terhubung pada file 'UserService.java' method 'registerUser()' untuk proses pendaftaran user di versi Web
        try {
            userService.registerUser(username, password);
            redirectAttributes.addFlashAttribute("message", "Registration successful. Please login.");
            return "redirect:/login";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/register";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    @GetMapping("/")
    public String index(@RequestParam(value = "sessionId", required = false) Long sessionId, Model model, HttpSession session) {
        UserEntity user = (UserEntity) session.getAttribute("loggedInUser");
        if (user == null) {
            return "redirect:/login";
        }

        user = userService.findById(user.getId()).orElse(null);
        if (user == null) return "redirect:/logout";

        session.setAttribute("loggedInUser", user);

        List<ChatSessionEntity> allSessions = chatService.getUserSessions(user);
        ChatSessionEntity currentSession = null;

        if (allSessions.isEmpty()) {
            currentSession = chatService.createSession("General Room", true, user);
            allSessions.add(currentSession);
        } else {
            if (sessionId != null) {
                currentSession = chatService.getSession(sessionId).orElse(allSessions.get(0));
            } else {
                currentSession = allSessions.get(0);
            }
        }

        model.addAttribute("title", "Second Brain Interface");
        model.addAttribute("memorySize", agent.getMemorySize(user.getId()));
        model.addAttribute("allSessions", allSessions);
        model.addAttribute("currentSession", currentSession);
        model.addAttribute("chatMessages", chatService.getMessages(currentSession.getId()));
        model.addAttribute("files", agent.getDocuments(user.getId()));
        model.addAttribute("dbFiles", agent.getDocumentEntities(user.getId()));
        model.addAttribute("assignedDocIds", currentSession != null ? chatService.getAssignedDocumentIds(currentSession.getId()) : new ArrayList<>());
        if (currentSession != null && currentSession.isGroupChat()) {
            model.addAttribute("groupDocs", chatService.getAssignedDocumentsWithUploader(currentSession.getId(), user.getId()));
        }
        model.addAttribute("user", user); 
        return "index";
    }

    @PostMapping("/upload")
    public String handleFileUpload(@RequestParam("file") MultipartFile file,
            HttpSession session, RedirectAttributes redirectAttributes) {
        // Kode ini terhubung pada file 'SecondBrainAgent.java' method 'learn()' untuk mengekstrak dan menyimpan referensi PDF di versi Web
        UserEntity user = (UserEntity) session.getAttribute("loggedInUser");
        if (user == null) {
            return "redirect:/login";
        }

        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("message", "Please select a file to upload");
            return "redirect:/";
        }

        try {
            // Save file to a permanent location
            Path uploadDir = java.nio.file.Paths.get(System.getProperty("user.dir"), "uploads");
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            Path destinationFile = uploadDir.resolve(file.getOriginalFilename());
            Files.copy(file.getInputStream(), destinationFile, StandardCopyOption.REPLACE_EXISTING);
            Path tempFile = destinationFile;

            // Process with SecondBrain
            PDFDocument pdfDoc = new PDFDocument();
            pdfDoc.setFilePath(tempFile);
            pdfDoc.setDetails("UPLOAD-" + System.currentTimeMillis(), file.getOriginalFilename(), user.getId());

            agent.learn(pdfDoc);

            redirectAttributes.addFlashAttribute("message", "Successfully learned from: " + file.getOriginalFilename());

        } catch (IOException e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("message", "Failed to upload file: " + e.getMessage());
        }

        return "redirect:/";
    }

    @PostMapping("/ask")
    public String askAgent(@RequestParam("question") String question,
            @RequestParam("sessionId") Long sessionId,
            HttpSession session, RedirectAttributes redirectAttributes) {
        UserEntity user = (UserEntity) session.getAttribute("loggedInUser");
        if (user == null) {
            return "redirect:/login";
        }

        if (question != null && !question.trim().isEmpty() && sessionId != null) {
            chatService.sendMessage(sessionId, user, question);
        }

        return "redirect:/?sessionId=" + sessionId;
    }

    @PostMapping("/chat/create")
    public String createChat(@RequestParam("title") String title, @RequestParam("isGroup") boolean isGroup, HttpSession session) {
        UserEntity user = (UserEntity) session.getAttribute("loggedInUser");
        if (user != null && title != null && !title.trim().isEmpty()) {
            ChatSessionEntity newSess = chatService.createSession(title, isGroup, user);
            return "redirect:/?sessionId=" + newSess.getId();
        }
        return "redirect:/";
    }

    @PostMapping("/chat/rename")
    public String renameChat(@RequestParam("sessionId") Long sessionId, @RequestParam("newTitle") String newTitle, HttpSession session) {
        UserEntity user = (UserEntity) session.getAttribute("loggedInUser");
        if (user != null && newTitle != null && !newTitle.trim().isEmpty()) {
            chatService.renameSession(sessionId, newTitle);
        }
        return "redirect:/?sessionId=" + sessionId;
    }

    @PostMapping("/chat/delete")
    public String deleteChat(@RequestParam("sessionId") Long sessionId, HttpSession session) {
        UserEntity user = (UserEntity) session.getAttribute("loggedInUser");
        if (user != null) {
            chatService.deleteSession(sessionId);
        }
        return "redirect:/";
    }

    @PostMapping("/chat/invite")
    public String inviteUser(@RequestParam("sessionId") Long sessionId, @RequestParam("username") String username, HttpSession session, RedirectAttributes redirectAttributes) {
        UserEntity user = (UserEntity) session.getAttribute("loggedInUser");
        if (user != null && username != null && !username.trim().isEmpty()) {
            Optional<UserEntity> userToInvite = userService.findByUsername(username);
            if (userToInvite.isPresent()) {
                chatService.joinGroup(sessionId, userToInvite.get());
                redirectAttributes.addFlashAttribute("message", "User '" + username + "' has been invited to the group.");
            } else {
                redirectAttributes.addFlashAttribute("message", "User '" + username + "' not found.");
            }
        }
        return "redirect:/?sessionId=" + sessionId;
    }

    @PostMapping("/chat/document/assign")
    public String assignDocument(@RequestParam("sessionId") Long sessionId, @RequestParam("docId") Long docId, HttpSession session) {
        UserEntity user = (UserEntity) session.getAttribute("loggedInUser");
        if (user != null && sessionId != null && docId != null) {
            chatService.assignDocumentToSession(sessionId, docId);
        }
        return "redirect:/?sessionId=" + sessionId;
    }

    @PostMapping("/chat/document/remove")
    public String removeDocument(@RequestParam("sessionId") Long sessionId, @RequestParam("docId") Long docId, HttpSession session) {
        UserEntity user = (UserEntity) session.getAttribute("loggedInUser");
        if (user != null && sessionId != null && docId != null) {
            chatService.removeDocumentFromSession(sessionId, docId);
        }
        return "redirect:/?sessionId=" + sessionId;
    }

    @GetMapping("/chat/messages/api")
    @org.springframework.web.bind.annotation.ResponseBody
    public List<java.util.Map<String, String>> getMessagesApi(@RequestParam("sessionId") Long sessionId, HttpSession session) {
        UserEntity user = (UserEntity) session.getAttribute("loggedInUser");
        if (user == null) return new ArrayList<>();
        
        List<com.secondbrain.persistence.ChatMessageEntity> msgs = chatService.getMessages(sessionId);
        List<java.util.Map<String, String>> result = new ArrayList<>();
        for(com.secondbrain.persistence.ChatMessageEntity m : msgs) {
            java.util.Map<String, String> map = new java.util.HashMap<>();
            map.put("sender", m.getSenderName());
            map.put("content", m.getMessageContent());
            result.add(map);
        }
        return result;
    }

    @GetMapping("/subscription")
    public String subscriptionPage(HttpSession session, Model model) {
        UserEntity user = (UserEntity) session.getAttribute("loggedInUser");
        if (user == null)
            return "redirect:/login";
        model.addAttribute("user", user);
        return "subscription";
    }

    @PostMapping("/subscription/confirm")
    public String upgradeSubscription(HttpSession session, RedirectAttributes redirectAttributes) {
        UserEntity user = (UserEntity) session.getAttribute("loggedInUser");
        if (user == null)
            return "redirect:/login";

        userService.upgradeSubscription(user, "PRO", 1000);
        redirectAttributes.addFlashAttribute("message", "Successfully upgraded to PRO!");
        return "redirect:/";
    }

    @GetMapping("/files/{filename:.+}")
    @org.springframework.web.bind.annotation.ResponseBody
    public org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> serveFile(
            @org.springframework.web.bind.annotation.PathVariable String filename, HttpSession session) {
        UserEntity user = (UserEntity) session.getAttribute("loggedInUser");
        if (user == null)
            return org.springframework.http.ResponseEntity.status(401).build();

        try {
            for (com.secondbrain.core.DocumentSource doc : agent.getDocuments(user.getId())) {
                if (doc.getFilename().equals(filename)) {
                    if (doc instanceof PDFDocument) {
                        Path file = ((PDFDocument) doc).getFilePath();
                        org.springframework.core.io.Resource resource = new org.springframework.core.io.UrlResource(
                                file.toUri());
                        if (resource.exists() || resource.isReadable()) {
                            return org.springframework.http.ResponseEntity.ok()
                                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                                            "attachment; filename=\"" + resource.getFilename() + "\"")
                                    .body(resource);
                        }
                    }
                }
            }
            return org.springframework.http.ResponseEntity.notFound().build();
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.internalServerError().build();
        }
    }
}
