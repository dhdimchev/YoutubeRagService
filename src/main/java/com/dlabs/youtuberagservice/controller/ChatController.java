package com.dlabs.youtuberagservice.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/chat")
class ChatController {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    public ChatController(ChatClient.Builder builder, VectorStore vectorStore, @Value("classpath:/prompts/system-prompt.st") Resource systemPrompt) {
        this.vectorStore = vectorStore;
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

        return chatClient.prompt()
                .user("Using the provided Core Summaries from the video summaries of " + targetDate +
                      ", create a concise daily digest of the main points. " +
                      "Group the insights by major themes and highlight the most important takeaways.")
                .advisors(QuestionAnswerAdvisor.builder(vectorStore)
                        .searchRequest(SearchRequest.builder()
                                .filterExpression("ingestion_date == '" + targetDate + "' && header_2 == 'Core Summary'")
                                .topK(40) // Retrieve more documents for the digest
                                .build())
                        .build())
                .call()
                .content();
    }

}
