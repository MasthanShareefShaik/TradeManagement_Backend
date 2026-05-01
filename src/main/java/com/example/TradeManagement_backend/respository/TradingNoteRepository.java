package com.example.TradeManagement_backend.respository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.TradeManagement_backend.entity.TradingNote;

@Repository
public interface TradingNoteRepository extends JpaRepository<TradingNote, Long> {
	 List<TradingNote> findByDate(LocalDate date);
}
