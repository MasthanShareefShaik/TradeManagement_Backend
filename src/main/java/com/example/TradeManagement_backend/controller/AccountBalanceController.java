package com.example.TradeManagement_backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.TradeManagement_backend.entity.AccountBalance;
import com.example.TradeManagement_backend.service.AccountBalanceService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/balance")
@RequiredArgsConstructor
public class AccountBalanceController {

    private final AccountBalanceService service;

    @GetMapping
    public AccountBalance getBalance() {
        return service.getBalance();
    }

    @PostMapping("/update")
    public AccountBalance update(@RequestBody Double amount) {
        return service.manualUpdate(amount);
    }
}