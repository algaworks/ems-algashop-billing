package com.algaworks.algashop.billing.infrastructure.kafka;

import com.algaworks.algashop.billing.domain.model.DomainEntityNotFoundException;
import com.algaworks.algashop.billing.domain.model.DomainException;
import jakarta.validation.ConstraintViolationException;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

import java.time.Duration;
import java.util.Map;

@Configuration
public class KafkaConfig {

    private static final String DLT_PREFIX = "billing.dlt.";

    public static final String TYPE_ID_HEADER = "__TypeId__";
    public static final String IDEMPOTENCY_KEY_HEADER = "idempotency-key";

    private static final int TOPIC_PARTITIONS = 3;
    private static final int TOPIC_REPLICAS = 3;
    private static final long RETENTION_30_DAYS = Duration.ofDays(30).toMillis();

    @Bean
    public NewTopic invoiceEventTopic(AlgaShopMessagingKafkaProperties properties) {
        return TopicBuilder.name(properties.getInvoiceEventTopicName())
                .partitions(TOPIC_PARTITIONS)
                .replicas(TOPIC_REPLICAS)
                .configs(Map.of("min.insync.replicas", "2"))
                .build();
    }

    @Bean
    public NewTopic orderEventsDlt(AlgaShopMessagingKafkaProperties properties) {
        return deadLetterTopic(properties.getOrderEventTopicName());
    }

    @Bean
    public NewTopic invoiceEventsDlt(AlgaShopMessagingKafkaProperties properties) {
        return deadLetterTopic(properties.getInvoiceEventTopicName());
    }

    private NewTopic deadLetterTopic(String sourceTopic) {
        return TopicBuilder.name(DLT_PREFIX + sourceTopic)
                .partitions(TOPIC_PARTITIONS)
                .replicas(TOPIC_REPLICAS)
                .configs(Map.of(
                        "min.insync.replicas", "2",
                        "retention.ms", String.valueOf(RETENTION_30_DAYS)))
                .build();
    }

    @Bean
    public DeadLetterPublishingRecoverer deadLetterPublishingRecoverer(
            KafkaTemplate<String, Object> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, exception) -> new TopicPartition(DLT_PREFIX + record.topic(), record.partition()));
        recoverer.setFailIfSendResultIsError(false);
        recoverer.setLogRecoveryRecord(true);
        return recoverer;
    }

    @Bean
    public DefaultErrorHandler defaultErrorHandler(DeadLetterPublishingRecoverer recoverer) {
        ExponentialBackOff backOff = new ExponentialBackOff(2_000L, 2);
        backOff.setMaxInterval(8_000L);
        backOff.setMaxAttempts(3);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);
        errorHandler.addNotRetryableExceptions(
                DomainException.class,
                DomainEntityNotFoundException.class,
                ConstraintViolationException.class,
                DataIntegrityViolationException.class,
                IllegalArgumentException.class);
        return errorHandler;
    }
}
