package com.example.TradeManagement_backend.controller;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import com.example.TradeManagement_backend.entity.TradingNote;
import com.example.TradeManagement_backend.service.TradingNoteService;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/notes")
@RequiredArgsConstructor
@CrossOrigin
public class TradingNoteController {

    private final TradingNoteService service;

    @GetMapping
    public List<TradingNote> getAll() {
        return service.getAll();
    }

    @PostMapping
    public TradingNote create(@RequestBody TradingNote note) {
        return service.create(note);
    }

    @PutMapping("/{id}")
    public TradingNote update(@PathVariable Long id, @RequestBody TradingNote note) {
        return service.update(id, note);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
    @GetMapping("/by-date")
    public List<TradingNote> getByDate(@RequestParam String date) {
        return service.getByDate(LocalDate.parse(date));
    }
}