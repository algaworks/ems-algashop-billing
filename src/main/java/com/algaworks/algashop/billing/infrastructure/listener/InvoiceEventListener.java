package com.algaworks.algashop.billing.infrastructure.listener;

import com.algaworks.algashop.billing.application.invoice.event.InvoiceCanceledIntegrationEvent;
import com.algaworks.algashop.billing.application.invoice.event.InvoiceIntegrationEventPublisher;
import com.algaworks.algashop.billing.application.invoice.event.InvoiceIssuedIntegrationEvent;
import com.algaworks.algashop.billing.application.invoice.event.InvoicePaidIntegrationEvent;
import com.algaworks.algashop.billing.application.utility.Mapper;
import com.algaworks.algashop.billing.domain.model.invoice.InvoiceCanceledEvent;
import com.algaworks.algashop.billing.domain.model.invoice.InvoiceIssuedEvent;
import com.algaworks.algashop.billing.domain.model.invoice.InvoicePaidEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class InvoiceEventListener {

    private final Mapper mapper;
    private final InvoiceIntegrationEventPublisher invoiceIntegrationEventPublisher;

    @TransactionalEventListener
    public void listen(InvoiceIssuedEvent event) {
        invoiceIntegrationEventPublisher.send(
                mapper.convert(event, InvoiceIssuedIntegrationEvent.class));
    }

    @TransactionalEventListener
    public void listen(InvoiceCanceledEvent event) {
        invoiceIntegrationEventPublisher.send(mapper.convert(event, InvoiceCanceledIntegrationEvent.class));
    }

    @TransactionalEventListener
    public void listen(InvoicePaidEvent event) {
        invoiceIntegrationEventPublisher.send(mapper.convert(event, InvoicePaidIntegrationEvent.class));
    }

}
