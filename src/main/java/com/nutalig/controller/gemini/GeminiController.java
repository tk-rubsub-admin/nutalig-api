package com.nutalig.controller.gemini;

import com.nutalig.service.GeminiService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/gemini")
public class GeminiController {

    private final GeminiService geminiService;

    public GeminiController(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    @PostMapping("/ask")
    public String ask(@RequestBody String prompt) {
        return geminiService.ask(prompt);
    }
}