package com.algaworks.algashop.billing.application.invoice.reply;

import com.algaworks.algashop.billing.application.OutboundIntegrationReply;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InvoicePaidIntegrationReply implements OutboundIntegrationReply {

    @NotNull
    private UUID invoiceId;

    @NotBlank
    private String orderId;

    @NotNull
    private OffsetDateTime paidAt;

    @Override
    public String getAggregateId() {
        return orderId;
    }
}