package com.example.TradeManagement_backend.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.TradeManagement_backend.entity.TradeEntry;
import com.example.TradeManagement_backend.service.TradeService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("trade")
@RequiredArgsConstructor
public class TradeController {

	private final TradeService tradeService;
    @PostMapping
    public TradeEntry addTrade(@RequestBody TradeEntry trade) {
        return tradeService.addTrade(trade);
    }

    @GetMapping
    public List<TradeEntry> getAllTrades() {
        return tradeService.getAllTrades();
    }

    @PutMapping("/{id}")
    public TradeEntry updateTrade(@PathVariable Long id, @RequestBody TradeEntry trade) {
        return tradeService.updateTrade(id, trade);
    }

    @DeleteMapping("/{id}")
    public void deleteTrade(@PathVariable Long id) {
        tradeService.deleteTrade(id);
    }
    @GetMapping("/by-date")
    public List<TradeEntry> getByDate(@RequestParam String date) {
        LocalDate localDate = LocalDate.parse(date);
        return tradeService.getTradesByDate(localDate);
    }

@GetMapping("/download")
public ResponseEntity<byte[]> downloadTradesPdf(
        @RequestParam(required = false) String date) {

    LocalDate parsedDate = null;

    if (date != null && !date.isEmpty()) {
        parsedDate = LocalDate.parse(date);
    }

    byte[] pdf = tradeService.generateTradesPdf(parsedDate);

    return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=trades.pdf")
            .contentType(MediaType.APPLICATION_PDF)
            .body(pdf);
}
}
