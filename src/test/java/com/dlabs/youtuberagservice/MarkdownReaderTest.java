
package com.dlabs.youtuberagservice;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.util.List;

public class MarkdownReaderTest {
    public static void main(String[] args) {
        Resource resource = new FileSystemResource("src/test/resources/viewtest.md");
        MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
                .withAdditionalMetadata("ingestion_date", "2026-04-04")
                .withAdditionalMetadata("source", "viewtest.md")
                .build();

        MarkdownDocumentReader reader = new MarkdownDocumentReader(resource, config);
        List<Document> documents = reader.get();

        System.out.println("Total documents: " + documents.size());
        for (Document doc : documents) {
            System.out.println("Metadata: " + doc.getMetadata());
            System.out.println("Content: " + doc.getText());
            System.out.println("---");
        }
    }
}
