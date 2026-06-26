package com.mybill.MyBill_Backend.dto.sync;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RejectedChangeDto {
    private String changeId;
    private String entityType;
    private String entityId;
    private String reason;
}
