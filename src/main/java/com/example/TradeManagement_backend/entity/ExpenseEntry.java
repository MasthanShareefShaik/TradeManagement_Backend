package com.example.TradeManagement_backend.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.example.TradeManagement_backend.utils.TransactionType;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter

public class ExpenseEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String personName;

    private Double amount;

    @Enumerated(EnumType.STRING)
    private TransactionType type; // ADD / WITHDRAW
    private String description;
    private LocalDate date;
    private LocalDateTime timestamp;
}
