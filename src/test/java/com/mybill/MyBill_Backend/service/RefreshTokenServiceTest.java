package com.mybill.MyBill_Backend.service;

import com.mybill.MyBill_Backend.entity.RefreshToken;
import com.mybill.MyBill_Backend.entity.Role;
import com.mybill.MyBill_Backend.entity.User;
import com.mybill.MyBill_Backend.repository.RefreshTokenRepository;
import com.mybill.MyBill_Backend.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RefreshTokenServiceTest {
    private final RefreshTokenRepository repository = mock(RefreshTokenRepository.class);
    private final JwtUtil jwtUtil = mock(JwtUtil.class);
    private final RefreshTokenService service = new RefreshTokenService(repository, jwtUtil);

    @Test
    void storesOnlyHashedRefreshTokenAndTracksExpiry() {
        User user = User.builder().email("owner@example.com").role(Role.OWNER).build();
        when(jwtUtil.generateToken(user.getEmail(), user.getRole())).thenReturn("access-token");
        ReflectionTestUtils.setField(service, "refreshExpirationMillis", 60_000L);

        RefreshTokenService.TokenPair tokens = service.issue(user);

        verify(repository).save(argThat(stored ->
                stored.getUser() == user
                        && !stored.getTokenHash().equals(tokens.refreshToken())
                        && stored.getTokenHash().matches("[0-9a-f]{64}")
                        && stored.getExpiresAt().isAfter(Instant.now())));
        assertThat(tokens.accessToken()).isEqualTo("access-token");
    }

    @Test
    void rotatesAnActiveRefreshTokenAndRejectsReuse() {
        User user = User.builder().email("owner@example.com").role(Role.OWNER).build();
        RefreshToken existing = new RefreshToken();
        existing.setUser(user);
        existing.setExpiresAt(Instant.now().plusSeconds(60));
        when(repository.findByTokenHashAndRevokedAtIsNull(any())).thenReturn(Optional.of(existing));
        when(jwtUtil.generateToken(user.getEmail(), user.getRole())).thenReturn("access-token");
        ReflectionTestUtils.setField(service, "refreshExpirationMillis", 60_000L);

        service.rotate("valid_refresh_token");

        assertThat(existing.getRevokedAt()).isNotNull();
        when(repository.findByTokenHashAndRevokedAtIsNull(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.rotate("valid_refresh_token"))
                .isInstanceOf(RefreshTokenService.InvalidRefreshTokenException.class);
    }
}
