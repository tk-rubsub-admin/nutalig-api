package com.nutalig.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class PurchaseOrderTimelineDto {
    private String purchaseOrderNo;
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
    }
}
