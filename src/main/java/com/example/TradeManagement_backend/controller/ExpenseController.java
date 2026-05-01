package com.example.TradeManagement_backend.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.TradeManagement_backend.entity.ExpenseEntry;
import com.example.TradeManagement_backend.service.ExpenseService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService service;

    @GetMapping
    public List<ExpenseEntry> getAll() {
        return service.getAll();
    }

    @PostMapping
    public ExpenseEntry add(@RequestBody ExpenseEntry entry) {
        return service.add(entry);
    }

    @PutMapping("/{id}")
    public ExpenseEntry update(@PathVariable Long id, @RequestBody ExpenseEntry entry) {
        return service.update(id, entry);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @GetMapping("/by-date")
    public List<ExpenseEntry> byDate(@RequestParam String date) {
        return service.getByDate(LocalDate.parse(date));
    }

    @GetMapping("/by-person")
    public List<ExpenseEntry> byPerson(@RequestParam String personName) {
        return service.getByPerson(personName);
    }
    @GetMapping("/download")
    public ResponseEntity<byte[]> downloadPdf() {
        byte[] pdf = service.generatePdf();

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=expenses.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}