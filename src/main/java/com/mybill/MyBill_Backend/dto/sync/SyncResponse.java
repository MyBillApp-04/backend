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

    public static SyncResponseBuilder builder() { return new SyncResponseBuilder(); }

    public static class SyncResponseBuilder {
        private LocalDateTime serverTime;
        private List<String> acceptedChangeIds;
        private List<RejectedChangeDto> rejected;
        private Map<String, Object> changes;
        private String nextCursor;
        private Boolean hasMore;
        private String conflictPolicy;
        private Integer conflictCount;

        public SyncResponseBuilder serverTime(LocalDateTime serverTime) { this.serverTime = serverTime; return this; }
        public SyncResponseBuilder acceptedChangeIds(List<String> acceptedChangeIds) { this.acceptedChangeIds = acceptedChangeIds; return this; }
        public SyncResponseBuilder rejected(List<RejectedChangeDto> rejected) { this.rejected = rejected; return this; }
        public SyncResponseBuilder changes(Map<String, Object> changes) { this.changes = changes; return this; }
        public SyncResponseBuilder nextCursor(String nextCursor) { this.nextCursor = nextCursor; return this; }
        public SyncResponseBuilder hasMore(Boolean hasMore) { this.hasMore = hasMore; return this; }
        public SyncResponseBuilder conflictPolicy(String conflictPolicy) { this.conflictPolicy = conflictPolicy; return this; }
        public SyncResponseBuilder conflictCount(Integer conflictCount) { this.conflictCount = conflictCount; return this; }

        public SyncResponse build() {
            SyncResponse response = new SyncResponse();
            response.serverTime = this.serverTime;
            response.acceptedChangeIds = this.acceptedChangeIds;
            response.rejected = this.rejected;
            response.changes = this.changes;
            response.nextCursor = this.nextCursor;
            response.hasMore = this.hasMore;
            response.conflictPolicy = this.conflictPolicy;
            response.conflictCount = this.conflictCount;
            return response;
        }
    }

    public LocalDateTime getServerTime() { return serverTime; }
    public void setServerTime(LocalDateTime serverTime) { this.serverTime = serverTime; }

    public List<String> getAcceptedChangeIds() { return acceptedChangeIds; }
    public void setAcceptedChangeIds(List<String> acceptedChangeIds) { this.acceptedChangeIds = acceptedChangeIds; }

    public List<RejectedChangeDto> getRejected() { return rejected; }
    public void setRejected(List<RejectedChangeDto> rejected) { this.rejected = rejected; }

    public Map<String, Object> getChanges() { return changes; }
    public void setChanges(Map<String, Object> changes) { this.changes = changes; }

    public String getNextCursor() { return nextCursor; }
    public void setNextCursor(String nextCursor) { this.nextCursor = nextCursor; }

    public Boolean getHasMore() { return hasMore; }
    public void setHasMore(Boolean hasMore) { this.hasMore = hasMore; }

    public String getConflictPolicy() { return conflictPolicy; }
    public void setConflictPolicy(String conflictPolicy) { this.conflictPolicy = conflictPolicy; }

    public Integer getConflictCount() { return conflictCount; }
    public void setConflictCount(Integer conflictCount) { this.conflictCount = conflictCount; }
}
