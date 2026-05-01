package com.example.TradeManagement_backend.respository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.TradeManagement_backend.entity.TradeEntry;

public interface TradeRepository extends JpaRepository<TradeEntry, Long> {
	   List<TradeEntry> findByTimestampBetween(LocalDateTime start, LocalDateTime end);
}
