package com.nutalig.dto.line;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class DeliveryContext {

    @JsonProperty("isRedelivery")
    private Boolean isRedelivery;

}
