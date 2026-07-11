package com.mybill.MyBill_Backend.dto.sync;

import lombok.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyncRequest {

    @Size(max = 120, message = "Device ID must be 120 characters or fewer")
    private String deviceId;

    private LocalDateTime lastPulledAt;

    @Valid
    @Size(max = 100, message = "Sync request can include at most 100 changes")
    private List<SyncChangeDto> changes;

    @Size(max = 500, message = "Sync cursor must be 500 characters or fewer")
    private String cursor;

    @Min(value = 1, message = "Page size must be at least 1")
    @Max(value = 500, message = "Page size must be 500 or fewer")
    private Integer pageSize;

    @Pattern(regexp = "^(CLIENT_WINS|SERVER_WINS)?$", message = "Conflict policy must be CLIENT_WINS or SERVER_WINS")
    private String conflictPolicy;

    private Boolean background;
}
