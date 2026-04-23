package com.nutalig.dto.line;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Message {

    @JsonProperty("type")
    private String type;
    @JsonProperty("id")
    private String id;
    @JsonProperty("quoteToken")
    private String quoteToken;
    @JsonProperty("markAsReadToken")
    private String markAsReadToken;
    @JsonProperty("text")
    private String text;

}
