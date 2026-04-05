package com.dlabs.youtuberagservice.repository;

import com.dlabs.youtuberagservice.domain.DailyDigest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface DailyDigestRepository extends JpaRepository<DailyDigest, LocalDate> {
}
