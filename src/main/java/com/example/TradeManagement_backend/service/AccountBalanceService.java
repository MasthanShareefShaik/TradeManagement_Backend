package com.example.TradeManagement_backend.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.example.TradeManagement_backend.entity.AccountBalance;
import com.example.TradeManagement_backend.respository.AccountBalanceRepository;

import lombok.RequiredArgsConstructor;
@Service
@RequiredArgsConstructor
public class AccountBalanceService {

    private final AccountBalanceRepository repo;

    public AccountBalance getBalance() {
        return repo.findById(1L)
                .orElseGet(() -> {
                    AccountBalance b = new AccountBalance();
                    b.setBalance(0.0);
                    b.setLastUpdated(LocalDateTime.now());
                    return repo.save(b);
                });
    }

    public void applyChange(Double amount) {
        AccountBalance b = getBalance();
        b.setBalance(b.getBalance() + amount);
        b.setLastUpdated(LocalDateTime.now());
        repo.save(b);
    }

    public AccountBalance manualUpdate(Double amount) {
        applyChange(amount); 
        return getBalance();
    }
}
