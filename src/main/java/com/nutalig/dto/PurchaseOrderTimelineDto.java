package com.nutalig.dto;

import com.nutalig.constant.PurchaseOrderStatus;
import com.nutalig.constant.PurchaseOrderMilestoneStatus;
import lombok.Data;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

@Data
public class PurchaseOrderTimelineDto {
    private String purchaseOrderNo;
    private PurchaseOrderStatus purchaseOrderStatus;
    private String supplierName;
    private CustomerDto customer;
    private Integer productionLeadTimeDay;
    private LocalDate productionStartedDate;
    private LocalDate productionExpectedEndDate;
    private LocalDate productionCompletedDate;
    private Boolean overdue;
    private List<Event> events;

    @Data
    public static class Event {
        private String type;
        private String label;
        private LocalDate date;
        private Boolean completed;
        private PurchaseOrderMilestoneStatus status;
        private LocalDate plannedDate;
        private ZonedDateTime actualAt;
        private String note;
        private Boolean optional;
        private Boolean overdue;
        private Boolean canComplete;
        private Boolean canSkip;
    }
}
