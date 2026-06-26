package com.mybill.MyBill_Backend.service;

import com.mybill.MyBill_Backend.entity.ActivityLog;
import com.mybill.MyBill_Backend.entity.User;
import com.mybill.MyBill_Backend.repository.ActivityLogRepository;
import com.mybill.MyBill_Backend.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;
    private final SecurityUtils securityUtils;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String action) {
        User user = securityUtils.getCurrentUser();

        ActivityLog activityLog = ActivityLog.builder()
                .user(user)
                .action(action)
                .build();

        activityLogRepository.save(activityLog);
    }
}
