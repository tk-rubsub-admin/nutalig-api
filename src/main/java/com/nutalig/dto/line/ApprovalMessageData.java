package com.nutalig.dto.line;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalMessageData {

    private Long requestId;
    private String requestNo;
    private String customerName;
    private String formattedAmount;
}
