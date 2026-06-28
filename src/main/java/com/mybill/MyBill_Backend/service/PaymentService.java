package com.mybill.MyBill_Backend.service;

import com.mybill.MyBill_Backend.entity.*;
import com.mybill.MyBill_Backend.repository.PaymentRepository;
import com.mybill.MyBill_Backend.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final SecurityUtils securityUtils;

    @Transactional(readOnly = true)
    public Page<Payment> getPaymentHistory(Pageable pageable) {
        return paymentRepository.findByUserIdAndIsDeletedFalseOrderByDateDesc(
                securityUtils.getCurrentUserId(),
                pageable
        );
    }
}
