package com.mybill.MyBill_Backend.service;

import com.mybill.MyBill_Backend.dto.ClientWorkDTO;
import com.mybill.MyBill_Backend.dto.ClientWorkProjection;
import com.mybill.MyBill_Backend.dto.DashboardStatsDTO;
import com.mybill.MyBill_Backend.dto.DashboardStatsProjection;
import com.mybill.MyBill_Backend.entity.ClientWork;
import com.mybill.MyBill_Backend.repository.ClientWorkRepository;
import com.mybill.MyBill_Backend.repository.DashboardRepository;
import com.mybill.MyBill_Backend.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final DashboardRepository dashboardRepository;
    private final ClientWorkRepository workRepository;
    private final SecurityUtils securityUtils;

    @Transactional(readOnly = true)
    @Cacheable(
            value = "dashboardStats",
            key = "#root.target.getDashboardCacheKey(#month, #year)",
            unless = "#result == null"
    )
    public DashboardStatsDTO getDashboardSummary(Integer month, Integer year) {
        Long userId = securityUtils.getCurrentUserId();
        LocalDate today = LocalDate.now();

        // Fallback to current year/month if parameters are not provided
        int targetYear = (year != null) ? year : today.getYear();
        int targetMonth = (month != null) ? month : today.getMonthValue();

        LocalDate baseDate = LocalDate.of(targetYear, targetMonth, 1);
        LocalDateTime monthStart = baseDate.atStartOfDay();
        LocalDateTime nextMonthStart = baseDate.plusMonths(1).atStartOfDay();

        DashboardStatsProjection stats = dashboardRepository.getDashboardStats(
                userId,
                monthStart,
                nextMonthStart
        );

        // NEW: Swapped to the optimized Projection fetch to avoid full entity graph hydration
        List<ClientWorkDTO> recentActivity = workRepository
                .findRecentActivityProjected(userId, PageRequest.of(0, 5))
                .stream()
                .map(this::toWorkDto)
                .toList();

        return DashboardStatsDTO.builder()
                .totalClients(valueOrZero(stats.getTotalClients()))
                .thisMonthBilled(valueOrZero(stats.getThisMonthBilled()))
                .thisMonthReceived(valueOrZero(stats.getThisMonthReceived()))
                .totalPending(valueOrZero(stats.getTotalPending()))
                .pendingInvoices(valueOrZero(stats.getPendingInvoices()))
                .topClient(stats.getTopClient() != null ? stats.getTopClient() : "N/A")
                .recentActivity(recentActivity)
                .build();
    }

    public String getDashboardCacheKey(Integer month, Integer year) {
        Long userId = securityUtils.getCurrentUserId();
        LocalDate today = LocalDate.now();

        int targetYear = (year != null) ? year : today.getYear();
        int targetMonth = (month != null) ? month : today.getMonthValue();

        return userId + ":" + targetYear + "-" + targetMonth;
    }

    // Maintained for backward compatibility if used anywhere else
    private ClientWorkDTO toWorkDto(ClientWork work) {
        return ClientWorkDTO.builder()
                .id(work.getId())
                .clientId(work.getClient() != null ? work.getClient().getId() : null)
                .clientName(work.getClient() != null ? work.getClient().getName() : null)
                .description(work.getDescription())
                .rate(work.getRate())
                .quantity(work.getQuantity())
                .amount(work.getAmount())
                .billed(work.getBilled())
                .isDeleted(work.getIsDeleted())
                .invoiceId(work.getInvoice() != null ? work.getInvoice().getId() : null)
                .workDate(work.getDate())
                .createdAt(work.getCreatedAt())
                .updatedAt(work.getUpdatedAt())
                .deletedAt(work.getDeletedAt())
                .build();
    }

    // NEW: Mapping straight from the lightning-fast DB Projection
    private ClientWorkDTO toWorkDto(ClientWorkProjection projection) {
        return ClientWorkDTO.builder()
                .id(projection.getId())
                .clientId(projection.getClientId())
                .clientName(projection.getClientName())
                .description(projection.getDescription())
                .rate(projection.getRate())
                .quantity(projection.getQuantity())
                .amount(projection.getAmount())
                .billed(projection.getBilled())
                .isDeleted(projection.getIsDeleted())
                .invoiceId(projection.getInvoiceId())
                .workDate(projection.getWorkDate())
                .createdAt(projection.getCreatedAt())
                .updatedAt(projection.getUpdatedAt())
                .deletedAt(projection.getDeletedAt())
                .build();
    }

    private long valueOrZero(Long value) {
        return value == null ? 0L : value;
    }

    private double valueOrZero(Double value) {
        return value == null ? 0.0 : value;
    }
}