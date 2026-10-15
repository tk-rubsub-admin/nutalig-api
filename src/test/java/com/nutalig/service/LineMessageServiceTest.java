package com.nutalig.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LineMessageServiceTest {

    @Test
    void rendersNotificationDetailsWithNewlinesAndJsonSpecialCharacters() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        LineMessageService service = new LineMessageService(null, null, objectMapper);
        String detail = "เลขที่ PO: PO-1\nลูกค้า: บริษัท \"ตัวอย่าง\" \\ จำกัด";

        JsonNode notification = service.renderNotificationTemplate(Map.of(
                "altText", "เริ่มผลิตแล้ว",
                "title", "เริ่มผลิตแล้ว",
                "detail", detail,
                "detailUrl", "https://example.com/sales-order/SO-1"
        ));

        JsonNode serializedAndParsed = objectMapper.readTree(objectMapper.writeValueAsBytes(notification));
        assertEquals(detail, serializedAndParsed.at("/contents/body/contents/1/contents/0/text").asText());
    }
}
