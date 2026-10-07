package com.algaworks.algashop.billing.infrastructure.listener;

import com.algaworks.algashop.billing.application.invoice.event.InvoiceCanceledIntegrationEvent;
import com.algaworks.algashop.billing.application.invoice.event.InvoiceIntegrationEventPublisher;
import com.algaworks.algashop.billing.application.invoice.event.InvoiceIssuedIntegrationEvent;
import com.algaworks.algashop.billing.application.invoice.event.InvoicePaidIntegrationEvent;
import com.algaworks.algashop.billing.application.invoice.reply.InvoiceCanceledIntegrationReply;
import com.algaworks.algashop.billing.application.invoice.reply.InvoiceIntegrationReplyPublisher;
import com.algaworks.algashop.billing.application.invoice.reply.InvoicePaidIntegrationReply;
import com.algaworks.algashop.billing.application.utility.Mapper;
import com.algaworks.algashop.billing.domain.model.invoice.InvoiceCanceledEvent;
import com.algaworks.algashop.billing.domain.model.invoice.InvoiceIssuedEvent;
import com.algaworks.algashop.billing.domain.model.invoice.InvoicePaidEvent;
import com.algaworks.algashop.billing.infrastructure.persistence.reply.PendingReplies;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class InvoiceEventListener {

    private final Mapper mapper;
    private final InvoiceIntegrationEventPublisher invoiceIntegrationEventPublisher;

    private final PendingReplies pendingReplies;
    private final InvoiceIntegrationReplyPublisher invoiceIntegrationReplyPublisher;

    @TransactionalEventListener
    public void listen(InvoiceIssuedEvent event) {
        invoiceIntegrationEventPublisher.send(
                mapper.convert(event, InvoiceIssuedIntegrationEvent.class));
    }

    @TransactionalEventListener
    public void listen(InvoiceCanceledEvent event) {
        invoiceIntegrationEventPublisher.send(mapper.convert(event, InvoiceCanceledIntegrationEvent.class));

        pendingReplies.find(event.getInvoiceId()).ifPresent(pending -> {
            invoiceIntegrationReplyPublisher.send(
                    pending.getReplyTopic(),
                    pending.getCorrelationId(),
                    InvoiceCanceledIntegrationReply.builder()
                            .invoiceId(event.getInvoiceId())
                            .orderId(event.getOrderId())
                            .canceledAt(event.getCanceledAt())
                            .build()
            );
            pendingReplies.remove(event.getInvoiceId());
        });
    }

    @TransactionalEventListener
    public void listen(InvoicePaidEvent event) {
        invoiceIntegrationEventPublisher.send(mapper.convert(event, InvoicePaidIntegrationEvent.class));

        pendingReplies.find(event.getInvoiceId()).ifPresent(pending -> {
            invoiceIntegrationReplyPublisher.send(
                    pending.getReplyTopic(),
                    pending.getCorrelationId(),
                    InvoicePaidIntegrationReply.builder()
                            .invoiceId(event.getInvoiceId())
                            .orderId(event.getOrderId())
                            .paidAt(event.getPaidAt())
                            .build()
            );
            pendingReplies.remove(event.getInvoiceId());
        });
    }

}
