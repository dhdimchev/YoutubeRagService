package com.dlabs.youtuberagservice.service;

import com.dlabs.youtuberagservice.domain.DailyDigest;
import com.dlabs.youtuberagservice.repository.DailyDigestRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;

@Service
public class DailyDigestService {

    private final DailyDigestRepository repository;
    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    public DailyDigestService(DailyDigestRepository repository, 
                              ChatClient.Builder builder, 
                              VectorStore vectorStore, 
                              @Value("classpath:/prompts/system-prompt.st") Resource systemPrompt) {
        this.repository = repository;
        this.vectorStore = vectorStore;
        this.chatClient = builder
                .defaultSystem(systemPrompt)
                .build();
    }
    public String getOrCreateDigest(LocalDate date) {
        return repository.findById(date).map(DailyDigest::getContent).orElseGet(() -> generateAndStore(date));
    }

    private String generateAndStore(LocalDate date) {
        String content = generateDigest(date);
        var digest = new DailyDigest(date, content);
        repository.save(digest);
        return content;
    }

    private String generateDigest(LocalDate date) {
        String targetDate = date.toString();
        return chatClient.prompt()
                .user("Using the provided Core Summaries from the video summaries of " + targetDate +
                        ", create a concise daily digest of the main points. " +
                        "Group the insights by major themes and highlight the most important takeaways.")
                .advisors(QuestionAnswerAdvisor.builder(vectorStore)
                        .searchRequest(SearchRequest.builder()
                                .filterExpression("ingestion_date == '" + targetDate + "' && header_2 == 'Core Summary'")
                                .topK(40)
                                .build())
                        .build())
                .call()
                .content();
    }
}
