package com.nutalig.dto.line;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Event {
    @JsonProperty("type")
    private String type;
    @JsonProperty("message")
    private Message message;
    @JsonProperty("webhookEventId")
    private String webhookEventId;
    @JsonProperty("deliveryContext")
    private DeliveryContext deliveryContext;
    @JsonProperty("timestamp")
    private Long timestamp;
    @JsonProperty("source")
    private Source source;
    @JsonProperty("replyToken")
    private String replyToken;
    @JsonProperty("mode")
    private String mode;
}
