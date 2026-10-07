package com.algaworks.algashop.billing.infrastructure.kafka;

import com.algaworks.algashop.billing.application.invoice.command.IssueInvoiceIntegrationCommand;
import com.algaworks.algashop.billing.application.invoice.management.GenerateInvoiceInput;
import com.algaworks.algashop.billing.application.invoice.management.GenerateInvoiceInputAssembler;
import com.algaworks.algashop.billing.application.invoice.management.InvoiceManagementApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@Slf4j
@RequiredArgsConstructor
@KafkaListener(
        id = "#{algaShopMessagingKafkaProperties.invoiceCommandsConsumerGroup}",
        concurrency = "3",
        topics = "#{algaShopMessagingKafkaProperties.invoiceCommandsTopicName}")
public class KafkaInvoiceIntegrationCommandListener {

    private final InvoiceManagementApplicationService invoiceManagementApplicationService;
    private final GenerateInvoiceInputAssembler generateInvoiceInputAssembler;

    @KafkaHandler
    public void handle(
            @Payload @Valid IssueInvoiceIntegrationCommand command,
            @Header(value = KafkaHeaders.CORRELATION_ID, required = false) byte[] correlationId,
            @Header(value = KafkaHeaders.REPLY_TOPIC, required = false) byte[] replyTopic,
            @Header(value = KafkaHeaders.RECEIVED_KEY, required = false) String messageKey,
            @Header(value = KafkaHeaders.RECEIVED_PARTITION, required = false) Integer partition,
            @Header(value = KafkaHeaders.OFFSET, required = false) Long offset) {
        logReceived(command, messageKey, partition, offset);

        requireHeader(correlationId, KafkaHeaders.CORRELATION_ID);
        requireHeader(replyTopic, KafkaHeaders.REPLY_TOPIC);

        GenerateInvoiceInput input = generateInvoiceInputAssembler.toGenerateInvoiceInput(command);

        invoiceManagementApplicationService.generate(input);

    }

    // o tópico é exclusivo de comandos de fatura: tipo desconhecido é erro de contrato
    @KafkaHandler(isDefault = true)
    public void handle(
            @Payload Object command,
            @Header(value = KafkaHeaders.RECEIVED_KEY, required = false) String messageKey,
            @Header(value = KafkaHeaders.OFFSET, required = false) Long offset) {
        log.warn("Unsupported invoice command: type={} key={} offset={}",
                command.getClass().getSimpleName(), messageKey, offset);
        throw new IllegalArgumentException(
                "Unsupported invoice command type " + command.getClass().getSimpleName());
    }

    private String requireHeader(byte[] value, String name) {
        if (value == null || value.length == 0) {
            throw new IllegalArgumentException("Invoice command without the " + name + " header");
        }
        return new String(value, StandardCharsets.UTF_8);
    }

    private void logReceived(Object command, String messageKey, Integer partition, Long offset) {
        log.info("Received {} | key={} | partition={} | offset={} | thread={}",
                command.getClass().getSimpleName(), messageKey, partition, offset,
                Thread.currentThread().getName());
    }
}