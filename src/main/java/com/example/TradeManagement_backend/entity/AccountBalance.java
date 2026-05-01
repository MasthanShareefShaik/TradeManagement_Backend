package com.example.TradeManagement_backend.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class AccountBalance {

    @Id
    private Long id = 1L; // always 1

    private Double balance;

    private LocalDateTime lastUpdated;
}