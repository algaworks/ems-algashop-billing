package com.algaworks.algashop.billing.infrastructure.persistence.reply;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PendingReplies {

	private final PendingReplyRepository repository;

	@Transactional
	public void register(UUID invoiceId, String orderId, String correlationId, String replyTopic) {
		repository.save(new PendingReplyEntity(invoiceId, orderId, correlationId, replyTopic));
	}

}
