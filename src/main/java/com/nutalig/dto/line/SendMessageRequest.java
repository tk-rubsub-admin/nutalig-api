package com.nutalig.dto.line;

import lombok.Data;

import java.util.List;

@Data
public class SendMessageRequest {
    private String to;
    private List<Msg> messages;
}
