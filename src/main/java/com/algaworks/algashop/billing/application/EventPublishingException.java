package com.algaworks.algashop.billing.application;

public class EventPublishingException extends RuntimeException {

    public EventPublishingException(String message, IntegrationEvent event, Throwable cause) {
        super("%s Event=%s AggregateId=%s".formatted(
                message,
                event.getClass().getSimpleName(),
                event.getAggregateId()), cause);
    }

}
