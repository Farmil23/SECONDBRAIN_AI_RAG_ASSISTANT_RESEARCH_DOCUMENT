package com.secondbrain.service;

import com.secondbrain.persistence.UserEntity;
import com.secondbrain.persistence.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @jakarta.annotation.PostConstruct
    public void initAdmin() {
        if (userRepository.findByUsername("farhan").isEmpty()) {
            UserEntity admin = new UserEntity("farhan", "pass123", "PRO", 9999, "ADMIN");
            userRepository.save(admin);
        }
        if (userRepository.findByUsername("userbiasa").isEmpty()) {
            UserEntity user = new UserEntity("userbiasa", "pass123", "FREE", 10, "USER");
            userRepository.save(user);
        }
    }

    public UserEntity registerUser(String username, String password) {
        // -> INFO: Method ini terhubung pada file 'UserRepository.java' untuk memastikan username unik dan menyimpan user baru ke database MySQL
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username is already taken.");
        }

        UserEntity newUser = new UserEntity(username, password, "FREE", 10);
        return userRepository.save(newUser);
    }

    public Optional<UserEntity> login(String username, String password) {
        // -> INFO: Method ini terhubung pada file 'UserRepository.java' untuk mencari dan memvalidasi kredensial login user
        Optional<UserEntity> userOpt = userRepository.findByUsername(username);
        if (userOpt.isPresent()) {
            UserEntity user = userOpt.get();
            if (user.getPassword().equals(password)) { 
                return Optional.of(user);
            }
        }
        return Optional.empty();
    }

    public boolean deductToken(UserEntity user) {
        // -> INFO: Method ini berfungsi untuk mengurangi saldo token user setiap kali fitur AI digunakan dan memperbarui database
        if (user.getTokens() > 0) {
            user.setTokens(user.getTokens() - 1);
            userRepository.save(user);
            return true;
        }
        return false;
    }

    
    // UPDATE: Mengganti tier dan menambah token
    public UserEntity upgradeSubscription(UserEntity user, String tier, int addedTokens) {
        // -> INFO: Method ini menangani proses upgrade langganan dan penambahan token user
        user.setSubscriptionTier(tier);
        user.setTokens(user.getTokens() + addedTokens);
        return userRepository.save(user);
    }

    public Optional<UserEntity> findById(Long id) {
        return userRepository.findById(id);
    }

    public java.util.List<UserEntity> findAllUsers() {
        return userRepository.findAll();
    }

    public Optional<UserEntity> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
}
