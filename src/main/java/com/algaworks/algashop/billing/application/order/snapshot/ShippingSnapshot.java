package com.algaworks.algashop.billing.application.order.snapshot;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ShippingSnapshot(
        @NotNull @PositiveOrZero BigDecimal cost,
        @NotNull LocalDate expectedDate,
        @NotNull @Valid RecipientSnapshot recipient,
        @NotNull @Valid AddressSnapshot address
) {
}