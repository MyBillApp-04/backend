package com.mybill.MyBill_Backend.service;

import com.mybill.MyBill_Backend.entity.AuthProvider;
import com.mybill.MyBill_Backend.entity.Role;
import com.mybill.MyBill_Backend.entity.User;
import com.mybill.MyBill_Backend.repository.UserRepository;
import com.mybill.MyBill_Backend.security.JwtUtil;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final JwtUtil jwtUtil = mock(JwtUtil.class);
    private final AuthService authService = new AuthService(userRepository, jwtUtil);

    @Test
    void createsNewFirebaseUserAndReturnsBackendJwt() {
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtUtil.generateToken("new@example.com", Role.CLIENT)).thenReturn("signed-jwt");

        String token = authService.firebaseLogin(
                "new@example.com", "", AuthProvider.LOCAL, Role.CLIENT);

        assertThat(token).isEqualTo("signed-jwt");
        verify(userRepository).save(argThat(user ->
                user.getEmail().equals("new@example.com")
                        && user.getName().equals("new")
                        && user.getProvider() == AuthProvider.LOCAL
                        && user.getRole() == Role.CLIENT));
    }

    @Test
    void createsNewUserWithProvidedName() {
        when(userRepository.findByEmail("named@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        authService.firebaseLogin(
                "named@example.com", "Named User", AuthProvider.GOOGLE, Role.CLIENT);

        verify(userRepository).save(argThat(user -> user.getName().equals("Named User")));
    }

    @Test
    void createsNewUserWithEmailPrefixWhenNameIsNull() {
        when(userRepository.findByEmail("prefix@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        authService.firebaseLogin(
                "prefix@example.com", null, AuthProvider.LOCAL, Role.CLIENT);

        verify(userRepository).save(argThat(user -> user.getName().equals("prefix")));
    }

    @Test
    void upgradesExistingUserProviderAndMissingProfileFields() {
        User existing = User.builder()
                .email("owner@example.com")
                .name("")
                .provider(AuthProvider.LOCAL)
                .role(null)
                .build();
        when(userRepository.findByEmail(existing.getEmail())).thenReturn(Optional.of(existing));
        when(userRepository.save(existing)).thenReturn(existing);
        when(jwtUtil.generateToken(existing.getEmail(), Role.CLIENT)).thenReturn("upgraded-jwt");

        String token = authService.firebaseLogin(
                existing.getEmail(), "Owner", AuthProvider.GOOGLE, Role.CLIENT);

        assertThat(token).isEqualTo("upgraded-jwt");
        assertThat(existing.getName()).isEqualTo("Owner");
        assertThat(existing.getProvider()).isEqualTo(AuthProvider.GOOGLE);
        assertThat(existing.getRole()).isEqualTo(Role.CLIENT);
    }

    @Test
    void preservesCompleteExistingProfile() {
        User existing = User.builder()
                .email("owner@example.com")
                .name("Original")
                .provider(AuthProvider.GOOGLE)
                .role(Role.ADMIN)
                .build();
        when(userRepository.findByEmail(existing.getEmail())).thenReturn(Optional.of(existing));
        when(userRepository.save(existing)).thenReturn(existing);
        when(jwtUtil.generateToken(existing.getEmail(), Role.ADMIN)).thenReturn("existing-jwt");

        authService.firebaseLogin(
                existing.getEmail(), "Replacement", AuthProvider.LOCAL, Role.CLIENT);

        assertThat(existing.getName()).isEqualTo("Original");
        assertThat(existing.getProvider()).isEqualTo(AuthProvider.GOOGLE);
        assertThat(existing.getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    void preservesMissingNameWhenIncomingNameIsNullAndProviderAlreadyGoogle() {
        User existing = User.builder()
                .email("owner@example.com")
                .name(null)
                .provider(AuthProvider.GOOGLE)
                .role(Role.CLIENT)
                .build();
        when(userRepository.findByEmail(existing.getEmail())).thenReturn(Optional.of(existing));
        when(userRepository.save(existing)).thenReturn(existing);

        authService.firebaseLogin(
                existing.getEmail(), null, AuthProvider.GOOGLE, Role.CLIENT);

        assertThat(existing.getName()).isNull();
        assertThat(existing.getProvider()).isEqualTo(AuthProvider.GOOGLE);
    }

    @Test
    void preservesMissingNameWhenIncomingNameIsBlank() {
        User existing = User.builder()
                .email("blank@example.com")
                .name("")
                .provider(AuthProvider.LOCAL)
                .role(Role.CLIENT)
                .build();
        when(userRepository.findByEmail(existing.getEmail())).thenReturn(Optional.of(existing));
        when(userRepository.save(existing)).thenReturn(existing);

        authService.firebaseLogin(
                existing.getEmail(), " ", AuthProvider.LOCAL, Role.CLIENT);

        assertThat(existing.getName()).isEmpty();
    }
}
