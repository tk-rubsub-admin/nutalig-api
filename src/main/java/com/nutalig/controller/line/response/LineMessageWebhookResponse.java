package com.nutalig.controller.line.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nutalig.dto.line.Event;
import lombok.Data;

import java.util.List;

@Data
public class LineMessageWebhookResponse {
    @JsonProperty("destination")
    private String destination;
    @JsonProperty("events")
    private List<Event> events;
}
