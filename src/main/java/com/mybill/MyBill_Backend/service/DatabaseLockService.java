package com.mybill.MyBill_Backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

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
        }
    }
}