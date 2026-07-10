package com.nutalig.dto.line;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public record LinePushMessageRequest(

        String to,

        List<LineMessage> messages

) {

    public record LineMessage(

            String type,

            String text,

            String altText,

            JsonNode contents

    ) {

    }

}
