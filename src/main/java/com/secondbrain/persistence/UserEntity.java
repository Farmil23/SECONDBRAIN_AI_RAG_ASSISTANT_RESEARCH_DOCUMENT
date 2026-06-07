package com.secondbrain.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // -> KONSEP PBO: ENCAPSULATION (Pembungkusan / Variabel Private). Data penting dilarang bocor dan diakses manual dari luar kelas.
    private String username;
    private String password;

    // Using String to store 'FREE' or 'PRO'
    private String subscriptionTier;

    // Tokens left for asking questions
    private int tokens;

    // Admin or User role
    private String role;

    public UserEntity() {
    }

    public UserEntity(String username, String password, String subscriptionTier, int tokens, String role) {
        this.username = username;
        this.password = password;
        this.subscriptionTier = subscriptionTier;
        this.tokens = tokens;
        this.role = role != null ? role : "USER";
    }

    public UserEntity(String username, String password, String subscriptionTier, int tokens) {
        this(username, password, subscriptionTier, tokens, "USER");
    }

    public Long getId() {
        return id;
    }

    // -> KONSEP PBO: ENCAPSULATION (Getter/Setter). Satu-satunya cara aman sebuah objek luar memanggil data yang di-*private*.
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getSubscriptionTier() {
        return subscriptionTier;
    }

    public void setSubscriptionTier(String subscriptionTier) {
        this.subscriptionTier = subscriptionTier;
    }

    public int getTokens() {
        return tokens;
    }

    public void setTokens(int tokens) {
        this.tokens = tokens;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
