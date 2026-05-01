package com.example.TradeManagement_backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.example.TradeManagement_backend.entity.TradingNote;
import com.example.TradeManagement_backend.respository.TradingNoteRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TradingNoteService {

    private final TradingNoteRepository repository;

    public List<TradingNote> getAll() {
        return repository.findAll();
    }

    public TradingNote create(TradingNote note) {
        note.setTimestamp(LocalDateTime.now());
        return repository.save(note);
    }

    public TradingNote update(Long id, TradingNote note) {
        TradingNote existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Note not found"));

        existing.setContent(note.getContent());
        existing.setDate(note.getDate());
        existing.setTimestamp(LocalDateTime.now());

        return repository.save(existing);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
    public List<TradingNote> getByDate(LocalDate date) {
        return repository.findByDate(date);
    }
}
