package com.algaworks.algashop.billing.application;

import lombok.Getter;

@Getter
public class EventPublishingException extends RuntimeException {

	private IntegrationEvent event;

	public EventPublishingException() {
	}

	public EventPublishingException(String message) {
		super(message);
	}

	public EventPublishingException(String message, Throwable cause) {
		super(message, cause);
	}

	public EventPublishingException(Throwable cause) {
		super(cause);
	}

	public EventPublishingException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public EventPublishingException(String message,
	                                IntegrationEvent event,
	                                Throwable e) {
		super("%s Event=%s AggregateId=%s".formatted(message, event.getClass().getSimpleName(), event.getAggregateId()), e);
		this.event = event;
	}
}
