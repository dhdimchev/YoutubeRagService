package com.dlabs.youtuberagservice.controller;

import com.dlabs.youtuberagservice.service.IngestionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ingest")
class IngestionController {

    IngestionService ingestionService;

    public IngestionController(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping
    public ResponseEntity<Void> ingest(
            @RequestParam("files") List<MultipartFile> files
    ) {
        ingestionService.process(files);
        return ResponseEntity.accepted().build();
    }

}
