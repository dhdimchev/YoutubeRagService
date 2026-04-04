package com.dlabs.youtuberagservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class IngestionService {

    private static final Logger logger = LoggerFactory.getLogger(IngestionService.class);

    private final VectorStore vectorStore;

    private final TokenTextSplitter splitter = TokenTextSplitter.builder()
            .withChunkSize(500)
            .build();

    public IngestionService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public void process(List<MultipartFile> files) {
        var today = LocalDate.now().toString();

        for (var file : files) {
            var filename = file.getOriginalFilename();
            logger.info("Ingesting file: {}", filename);

            try {
                var rawDocs = readMarkdown(file, today, filename);

                if (rawDocs.isEmpty()) {
                    continue;
                }

                var structuredDocs = buildStructuredDocuments(rawDocs);

                logger.info("Final chunks for {}: {}", filename, structuredDocs.size());

                structuredDocs.forEach(doc ->
                        logger.debug("Embedding chunk: {}", preview(doc.getText()))
                );

                vectorStore.accept(structuredDocs);

            } catch (Exception e) {
                logger.error("Failed ingestion: {}", filename, e);
            }
        }
    }

    private List<Document> readMarkdown(MultipartFile file, String today, String filename) {
        var config = MarkdownDocumentReaderConfig.builder()
                .withAdditionalMetadata("ingestion_date", today)
                .withAdditionalMetadata("source", filename)
                .build();

        return new MarkdownDocumentReader(file.getResource(), config)
                .get()
                .stream()
                .filter(d -> d.getText() != null && !d.getText().isBlank())
                .toList();
    }

    private List<Document> buildStructuredDocuments(List<Document> docs) {
        // Group by source filename
        var grouped = docs.stream()
                .collect(Collectors.groupingBy(d ->
                        (String) d.getMetadata().getOrDefault("source", "unknown")
                ));

        var result = new ArrayList<Document>();

        for (var group : grouped.values()) {
            var sections = extractSections(group);
            var metadata = new HashMap<>(group.get(0).getMetadata());

            // Main content for digest
            var main = join(
                    sections.get("Topic"),
                    sections.get("Core Summary"),
                    sections.get("Detailed Summary"),
                    sections.get("Key Insights")
            );

            if (isValid(main)) {
                var mainMeta = new HashMap<>(metadata);
                // Tag for daily digest retrieval
                mainMeta.put("header_2", "Core Summary");
                result.addAll(splitIfNeeded(main, mainMeta));
            }

            // Facts and details
            var facts = join(
                    sections.get("Notable Details"),
                    sections.get("Claims or Opinions")
            );

            if (isValid(facts)) {
                result.addAll(splitIfNeeded(facts, metadata));
            }

            var concepts = join(
                    sections.get("Key Concepts"),
                    sections.get("Entities and Terms")
            );

            if (isValid(concepts)) {
                result.add(new Document(sanitize(concepts), metadata));
            }
        }

        return result;
    }

    private Map<String, String> extractSections(List<Document> group) {
        var map = new HashMap<String, String>();
        for (var doc : group) {
            var title = (String) doc.getMetadata().get("title");
            if (title != null) {
                map.put(title.trim(), doc.getText());
            }
        }
        return map;
    }

    private List<Document> splitIfNeeded(String text, Map<String, Object> metadata) {
        var cleanedText = sanitize(text);
        if (cleanedText.length() > 1500) {
            return splitter.apply(List.of(new Document(cleanedText, metadata)));
        }
        return List.of(new Document(cleanedText, metadata));
    }

    private String join(String... parts) {
        return Arrays.stream(parts)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.joining("\n\n"));
    }

    private String sanitize(String text) {
        if (text == null) return "";
        // Normalize text to avoid numerical issues
        return text.replaceAll("[\\p{Cntrl}&&[^\\n\\r\\t]]", "")
                   .replaceAll("\\s+", " ")
                   .trim();
    }

    private boolean isValid(String text) {
        return text != null && text.length() > 50;
    }

    private String preview(String text) {
        return text.length() > 120 ? text.substring(0, 120) + "..." : text;
    }
}