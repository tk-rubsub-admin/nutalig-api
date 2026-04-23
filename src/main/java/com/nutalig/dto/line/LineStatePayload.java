package com.nutalig.dto.line;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class LineStatePayload {

    String intent;
    String userId;
    String nonce;
}
