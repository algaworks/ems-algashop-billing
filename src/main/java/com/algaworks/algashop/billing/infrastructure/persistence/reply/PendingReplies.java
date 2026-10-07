package com.algaworks.algashop.billing.infrastructure.persistence.reply;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PendingReplies {

	private final PendingReplyRepository repository;

	@Transactional
	public void register(UUID invoiceId, String orderId, String correlationId, String replyTopic) {
		repository.save(new PendingReplyEntity(invoiceId, orderId, correlationId, replyTopic));
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
	public Optional<PendingReplyEntity> find(UUID invoiceId) {
		return repository.findById(invoiceId);
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void remove(UUID invoiceId) {
		repository.deleteById(invoiceId);
	}
}
