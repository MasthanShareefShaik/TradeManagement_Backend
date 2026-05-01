package com.example.TradeManagement_backend.entity;

import java.time.LocalDateTime;

import com.example.TradeManagement_backend.utils.ResultStatus;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumeratedValue;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import lombok.Getter;
import lombok.Setter;

@Entity
@Setter
@Getter
public class TradeEntry {
	
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE)
	@SequenceGenerator(allocationSize = 1,initialValue = 1)
	private Long id;
	private Double lots;
	private Double entryTrade;
	private Double exitTrade;
	private Double profitLossAmount;
	private String stockName;
	  private LocalDateTime timestamp; 
	  @Enumerated(EnumType.STRING)
	  private ResultStatus resultStatus;
	

}
