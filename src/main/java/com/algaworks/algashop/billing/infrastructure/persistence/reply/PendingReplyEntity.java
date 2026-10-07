package com.algaworks.algashop.billing.infrastructure.persistence.reply;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "pending_reply")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class PendingReplyEntity {

    @Id
    @EqualsAndHashCode.Include
    private UUID invoiceId;

    private String orderId;

    private String correlationId;

    private String replyTopic;

    private OffsetDateTime createdAt;

    public PendingReplyEntity(UUID invoiceId, String orderId, String correlationId, String replyTopic) {
        this.invoiceId = Objects.requireNonNull(invoiceId);
        this.orderId = Objects.requireNonNull(orderId);
        this.correlationId = Objects.requireNonNull(correlationId);
        this.replyTopic = Objects.requireNonNull(replyTopic);
        this.createdAt = OffsetDateTime.now();
    }
}