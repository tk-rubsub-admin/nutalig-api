package com.nutalig.dto.line;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Postback {
    @JsonProperty("data")
    private String data;
}
