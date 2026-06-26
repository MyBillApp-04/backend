package com.mybill.MyBill_Backend.dto.sync;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyncRequest {

    private String deviceId;

    private LocalDateTime lastPulledAt;

    private List<SyncChangeDto> changes;

    private String cursor;

    private Integer pageSize;

    private String conflictPolicy;

    private Boolean background;
}
