package com.nutalig.dto.line;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Source {
    @JsonProperty("type")
    private String type;
    @JsonProperty("userId")
    private String userId;
}
