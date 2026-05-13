package com.algaworks.algashop.billing.application.invoice.query;

import com.algaworks.algashop.billing.application.invoice.AbstractApplicationIT;
import com.algaworks.algashop.billing.domain.model.invoice.Invoice;
import com.algaworks.algashop.billing.domain.model.invoice.InvoiceNotFoundException;
import com.algaworks.algashop.billing.domain.model.invoice.InvoiceRepository;
import com.algaworks.algashop.billing.domain.model.invoice.InvoiceTestDataBuilder;
import com.algaworks.algashop.billing.domain.model.invoice.PaymentMethod;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

class InvoiceQueryServiceIT extends AbstractApplicationIT {

    @Autowired
    private InvoiceQueryService invoiceQueryService;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Test
    public void shouldFindByOrderId() {
        Invoice invoice = InvoiceTestDataBuilder.anInvoice().build();
        invoice.changePaymentSettings(PaymentMethod.GATEWAY_BALANCE, null);
        invoiceRepository.saveAndFlush(invoice);
        InvoiceOutput invoiceOutput = invoiceQueryService.findByOrderId(invoice.getOrderId());

        Assertions.assertThat(invoiceOutput.getId()).isEqualTo(invoice.getId());
    }

    @Test
    public void shouldFindByOrderIdAndCustomerId() {
        UUID customerId = UUID.randomUUID();
        Invoice invoice = InvoiceTestDataBuilder.anInvoice()
                .customerId(customerId)
                .paymentSettings(PaymentMethod.GATEWAY_BALANCE, null)
                .build();

        invoiceRepository.saveAndFlush(invoice);

        InvoiceOutput invoiceOutput = invoiceQueryService.findByOrderIdAndCustomerId(invoice.getOrderId(), customerId);

        Assertions.assertThat(invoiceOutput.getId()).isEqualTo(invoice.getId());
        Assertions.assertThat(invoiceOutput.getItems()).hasSize(invoice.getItems().size());
        Assertions.assertThat(invoiceOutput.getPaymentSettings().getMethod()).isEqualTo(PaymentMethod.GATEWAY_BALANCE);
    }

    @Test
    public void shouldNotFindByOrderIdAndDifferentCustomerId() {
        Invoice invoice = InvoiceTestDataBuilder.anInvoice()
                .paymentSettings(PaymentMethod.GATEWAY_BALANCE, null)
                .build();

        invoiceRepository.saveAndFlush(invoice);

        Assertions.assertThatExceptionOfType(InvoiceNotFoundException.class)
                .isThrownBy(() -> invoiceQueryService.findByOrderIdAndCustomerId(
                        invoice.getOrderId(), UUID.randomUUID()));
    }

}
