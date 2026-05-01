package com.example.TradeManagement_backend.respository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.TradeManagement_backend.entity.AccountBalance;

public interface AccountBalanceRepository extends JpaRepository<AccountBalance, Long> {

}
