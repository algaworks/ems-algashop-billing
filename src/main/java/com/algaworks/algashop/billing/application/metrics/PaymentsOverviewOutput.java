package com.algaworks.algashop.billing.application.metrics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.YearMonth;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentsOverviewOutput {
    private YearMonth yearMonth;
    private String paymentMethod;
    private Integer quantity;
    private BigDecimal totalAmount;
}
