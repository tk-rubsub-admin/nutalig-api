package com.nutalig.service;

import com.nutalig.client.OpenAiClient;
import com.nutalig.config.PromptTemplateEngine;
import com.nutalig.dto.ai.AiRequest;
import com.nutalig.dto.ai.AiResponse;
import com.nutalig.entity.AiPromptEntity;
import com.nutalig.exception.DataNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiExecutionService {

    private final PromptService promptService;
    private final PromptTemplateEngine templateEngine;
    private final OpenAiClient openAiClient;
    private final AiLoggingService loggingService;

    public String execute(String promptCode, Map<String, String> variables) throws DataNotFoundException {

        AiPromptEntity prompt = promptService.getActivePrompt(promptCode);

        String userPrompt =
                templateEngine.render(prompt.getUserPromptTemplate(), variables);

        AiRequest request = AiRequest.builder()
                .model(prompt.getModel())
                .temperature(prompt.getTemperature())
                .systemPrompt(prompt.getSystemPrompt())
                .userPrompt(userPrompt)
                .build();

        long start = System.currentTimeMillis();

        AiResponse response = openAiClient.call(request);

        long duration = System.currentTimeMillis() - start;

        loggingService.logPrompt(promptCode, response, duration);

        return response.getContent();
//        return """
//        {
//          "customerId": "บริษัท ทีเค รับทรัพย์ จำกัด",
//          "salesId": "กาญ",
//          "coSalesId": null,
//          "remark": "ลูกค้าได้ราคามา 15000",
//          "discount": 0,
//          "freight": 0,
//          "isVat": true,
//          "items": [
//            {
//              "name": "เครื่องคิ้มขวดน้ำหอม",
//              "type": null,
//              "capacity": null,
//              "size": "",
//              "spec": "",
//              "unitPrice": 10.5,
//              "quantity": 1
//            }
//          ]
//        }
//        """;
    }

}
