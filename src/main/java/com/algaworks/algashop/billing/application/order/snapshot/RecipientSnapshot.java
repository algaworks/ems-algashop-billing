package com.algaworks.algashop.billing.application.order.snapshot;

import jakarta.validation.constraints.NotBlank;

public record RecipientSnapshot(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank String document,
        @NotBlank String phone
) {
}