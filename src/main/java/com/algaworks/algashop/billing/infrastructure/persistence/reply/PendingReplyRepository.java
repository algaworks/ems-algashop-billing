package com.algaworks.algashop.billing.infrastructure.persistence.reply;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PendingReplyRepository extends JpaRepository<PendingReplyEntity, UUID> {
}
