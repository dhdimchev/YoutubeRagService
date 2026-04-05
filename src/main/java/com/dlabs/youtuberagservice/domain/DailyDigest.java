package com.dlabs.youtuberagservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "daily_digest")
public class DailyDigest {

    @Id
    @Column(name = "date")
    private LocalDate date;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    public DailyDigest() {}

    public DailyDigest(LocalDate date, String content) {
        this.date = date;
        this.content = content;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
