package com.mybill.MyBill_Backend.service;

import com.mybill.MyBill_Backend.observability.SecureLogMessageConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class DatabaseLockService {

    public static final long ASYNC_JOB_SCHEDULER = 7_410_001L;
    public static final long RECURRING_INVOICE_SCHEDULER = 7_410_002L;
    public static final long CUSTOMER_NOTIFICATION_RETRY = 7_410_003L;
    public static final long CUSTOMER_PAYMENT_REMINDER = 7_410_004L;

    private final JdbcTemplate jdbcTemplate;
    private final AtomicBoolean advisoryLocksAvailable = new AtomicBoolean(true);

    public boolean tryLock(long key) {
        if (!advisoryLocksAvailable.get()) {
            return true;
        }

        try {
            Boolean locked = jdbcTemplate.queryForObject("SELECT pg_try_advisory_lock(?)", Boolean.class, key);
            return Boolean.TRUE.equals(locked);
        } catch (RuntimeException ex) {
            advisoryLocksAvailable.set(false);
            log.debug("Database advisory locks are unavailable; falling back to local scheduler guard: exception={} message={}",
                    ex.getClass().getSimpleName(), SecureLogMessageConverter.sanitize(ex.getMessage()));
            return true;
        }
    }

    public void unlock(long key) {
        if (!advisoryLocksAvailable.get()) {
            return;
        }

        try {
            jdbcTemplate.queryForObject("SELECT pg_advisory_unlock(?)", Boolean.class, key);
        } catch (RuntimeException ex) {
            advisoryLocksAvailable.set(false);
            log.debug("Failed to release database advisory lock {}; disabling advisory locks for this runtime: exception={} message={}",
                    key, ex.getClass().getSimpleName(), SecureLogMessageConverter.sanitize(ex.getMessage()));
        }
    }
}
