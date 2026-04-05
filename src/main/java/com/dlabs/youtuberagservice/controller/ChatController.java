package com.dlabs.youtuberagservice.controller;

import com.dlabs.youtuberagservice.service.DailyDigestService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/chat")
class ChatController {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final DailyDigestService dailyDigestService;

    public ChatController(ChatClient.Builder builder, VectorStore vectorStore, @Value("classpath:/prompts/system-prompt.st") Resource systemPrompt, DailyDigestService dailyDigestService) {
        this.vectorStore = vectorStore;
        this.dailyDigestService = dailyDigestService;
        this.chatClient = builder
                .defaultSystem(systemPrompt)
                .defaultAdvisors(QuestionAnswerAdvisor.builder(vectorStore)
                        .searchRequest(SearchRequest.builder().topK(10).build())
                        .build())
                .build();
    }

    @GetMapping
    public String chat( @RequestParam(value = "message", defaultValue = "Tell me about the uploaded videos") String message,
            @RequestParam(value = "date", required = false) String date) {
        return chatClient.prompt()
                .user(message)
                .advisors(a -> {
                    if (date != null) {
                        a.param(QuestionAnswerAdvisor.FILTER_EXPRESSION, "ingestion_date == '" + date + "'");
                    }
                })
                .call()
                .content();
    }

    @GetMapping("/digest")
    public String digest(@RequestParam(value = "date", required = false) String date) {
        var targetDate = (date != null) ? date : LocalDate.now().toString();
        return dailyDigestService.getOrCreateDigest(LocalDate.parse(targetDate, java.time.format.DateTimeFormatter.ISO_LOCAL_DATE));
    }

}
