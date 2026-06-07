package com.secondbrain.service;

import com.secondbrain.persistence.UserEntity;
import com.secondbrain.persistence.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private UserEntity mockUser;

    @BeforeEach
    void setUp() {
        mockUser = new UserEntity("testuser", "testpass", "FREE", 10);
        ReflectionTestUtils.setField(mockUser, "id", 1L);
    }

    @Test
    void testRegisterUser_Success() {
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(userRepository.save(any(UserEntity.class))).thenAnswer(i -> i.getArguments()[0]);

        UserEntity result = userService.registerUser("newuser", "password");

        assertNotNull(result);
        assertEquals("newuser", result.getUsername());
        assertEquals("password", result.getPassword());
        assertEquals("FREE", result.getSubscriptionTier());
        assertEquals(10, result.getTokens());
        
        verify(userRepository, times(1)).findByUsername("newuser");
        verify(userRepository, times(1)).save(any(UserEntity.class));
        System.out.println("✅ [SUCCESS] Test registrasi user baru berjalan dan success");
    }

    @Test
    void testRegisterUser_UsernameTaken() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockUser));

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.registerUser("testuser", "password");
        });

        assertEquals("Username is already taken.", exception.getMessage());
        verify(userRepository, times(1)).findByUsername("testuser");
        verify(userRepository, never()).save(any(UserEntity.class));
        System.out.println("✅ [SUCCESS] Test registrasi dengan username yang sudah terpakai berjalan dan success");
    }

    @Test
    void testLogin_Success() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockUser));

        Optional<UserEntity> result = userService.login("testuser", "testpass");

        assertTrue(result.isPresent());
        assertEquals("testuser", result.get().getUsername());
        System.out.println("✅ [SUCCESS] Test login berhasil berjalan dan success");
    }

    @Test
    void testLogin_WrongPassword() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockUser));

        Optional<UserEntity> result = userService.login("testuser", "wrongpass");

        assertFalse(result.isPresent());
        System.out.println("✅ [SUCCESS] Test login dengan password salah berjalan dan success");
    }

    @Test
    void testLogin_UserNotFound() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        Optional<UserEntity> result = userService.login("unknown", "password");

        assertFalse(result.isPresent());
        System.out.println("✅ [SUCCESS] Test login dengan user tidak ditemukan berjalan dan success");
    }

    @Test
    void testDeductToken_Success() {
        assertTrue(userService.deductToken(mockUser));
        assertEquals(9, mockUser.getTokens());
        verify(userRepository, times(1)).save(mockUser);
        System.out.println("✅ [SUCCESS] Test pengurangan token berjalan dan success");
    }

    @Test
    void testDeductToken_InsufficientTokens() {
        mockUser.setTokens(0);

        assertFalse(userService.deductToken(mockUser));
        assertEquals(0, mockUser.getTokens());
        verify(userRepository, never()).save(mockUser);
        System.out.println("✅ [SUCCESS] Test pengurangan token gagal karena habis berjalan dan success");
    }

    @Test
    void testUpgradeSubscription() {
        when(userRepository.save(any(UserEntity.class))).thenAnswer(i -> i.getArguments()[0]);

        UserEntity result = userService.upgradeSubscription(mockUser, "PRO", 50);

        assertEquals("PRO", result.getSubscriptionTier());
        assertEquals(60, result.getTokens());
        verify(userRepository, times(1)).save(mockUser);
        System.out.println("✅ [SUCCESS] Test upgrade subscription berjalan dan success");
    }
}
