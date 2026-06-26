package com.mybill.MyBill_Backend.dto.sync;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyncResponse {

    private LocalDateTime serverTime;

    private List<String> acceptedChangeIds;

    private List<RejectedChangeDto> rejected;

    private Map<String, Object> changes;

    private String nextCursor;

    private Boolean hasMore;

    private String conflictPolicy;

    private Integer conflictCount;
}
