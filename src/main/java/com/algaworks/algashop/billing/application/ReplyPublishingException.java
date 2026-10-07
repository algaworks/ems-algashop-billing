package com.algaworks.algashop.billing.application;

public class ReplyPublishingException extends RuntimeException {

    public ReplyPublishingException(String message, OutboundIntegrationReply reply, Throwable cause) {
        super("%s Reply=%s AggregateId=%s".formatted(
                message,
                reply.getClass().getSimpleName(),
                reply.getAggregateId()), cause);
    }

}
