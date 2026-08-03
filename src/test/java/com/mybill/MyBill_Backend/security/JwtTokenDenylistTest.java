package com.mybill.MyBill_Backend.security;

import com.mybill.MyBill_Backend.entity.RevokedToken;
import com.mybill.MyBill_Backend.repository.RevokedTokenRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class JwtTokenDenylistTest {

    @Test
    void deniesActiveTokensByHashAndDoesNotStoreExpiredTokens() {
        JwtTokenDenylist denylist = new JwtTokenDenylist(100);

        denylist.deny("active-token", Date.from(Instant.now().plusSeconds(60)));
        denylist.deny("expired-token", Date.from(Instant.now().minusSeconds(1)));

        assertThat(denylist.isDenied("active-token")).isTrue();
        assertThat(denylist.isDenied("expired-token")).isFalse();
    }

    @Test
    void persistsDeniedTokensToRepositoryAndRecoversAcrossAppRestarts() {
        RevokedTokenRepository repository = mock(RevokedTokenRepository.class);
        when(repository.existsByTokenHash(any())).thenReturn(false);

        JwtTokenDenylist instance1 = new JwtTokenDenylist(100, repository);
        instance1.deny("session-token-1", Date.from(Instant.now().plusSeconds(300)));

        verify(repository, times(1)).save(any(RevokedToken.class));

        // Simulate app restart / new JVM instance with empty L1 Caffeine cache
        when(repository.existsByTokenHashAndExpiresAtAfter(any(), any(Instant.class))).thenReturn(true);

        JwtTokenDenylist instance2 = new JwtTokenDenylist(100, repository);
        assertThat(instance2.isDenied("session-token-1")).isTrue();

        verify(repository, times(1)).existsByTokenHashAndExpiresAtAfter(any(), any(Instant.class));
    }

    @Test
    void cleansUpExpiredTokensFromDatabase() {
        RevokedTokenRepository repository = mock(RevokedTokenRepository.class);
        when(repository.deleteExpiredTokens(any(Instant.class))).thenReturn(5);

        JwtTokenDenylist denylist = new JwtTokenDenylist(100, repository);
        int deleted = denylist.cleanupExpiredTokens();

        assertThat(deleted).isEqualTo(5);
        verify(repository, times(1)).deleteExpiredTokens(any(Instant.class));
    }
}
