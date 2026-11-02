package com.cloudledger.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class OutboxService {

    private static final Logger log = LoggerFactory.getLogger(OutboxService.class);

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher localEventPublisher;
    
    // Optional Kafka template (can be null if Kafka is not available in unit test environment)
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${cloudledger.outbox.enabled:true}")
    private boolean outboxEnabled;

    @Value("${cloudledger.outbox.batch-size:50}")
    private int batchSize;

    public OutboxService(OutboxRepository outboxRepository,
                         ObjectMapper objectMapper,
                         ApplicationEventPublisher localEventPublisher,
                         @org.springframework.beans.factory.annotation.Autowired(required = false) KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
        this.localEventPublisher = localEventPublisher;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Transactional
    public void recordEvent(String aggregateType, String aggregateId, String eventType, String correlationId, Object eventPayload) {
        try {
            String payloadJson = objectMapper.writeValueAsString(eventPayload);
            OutboxEvent event = new OutboxEvent(aggregateType, aggregateId, eventType, correlationId, payloadJson);
            outboxRepository.save(event);
            log.debug("Persisted outbox event: id={}, type={}, aggregate={}", event.getOutboxId(), eventType, aggregateId);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize outbox event payload", e);
        }
    }

    /**
     * Polling publisher that runs on a schedule to publish pending outbox events to Kafka
     * and local Spring application event bus for in-process decoupling.
     */
    @Scheduled(fixedDelayString = "${cloudledger.outbox.poll-interval-ms:1000}")
    @Transactional
    public void publishPendingOutboxEvents() {
        if (!outboxEnabled) {
            return;
        }

        List<OutboxEvent> pendingEvents = outboxRepository.findByStatusOrderByCreatedAtAsc("PENDING", PageRequest.of(0, batchSize));
        for (OutboxEvent event : pendingEvents) {
            try {
                // If Kafka is configured, publish to the event topic
                if (kafkaTemplate != null) {
                    kafkaTemplate.send(event.getEventType(), event.getAggregateId(), event.getPayload());
                }
                
                // Publish to local Spring event stream for seamless local execution
                localEventPublisher.publishEvent(event);

                event.setStatus("PUBLISHED");
                event.setDispatchedAt(Instant.now());
                outboxRepository.save(event);
                log.debug("Dispatched outbox event {}", event.getOutboxId());
            } catch (Exception e) {
                log.error("Failed to publish outbox event {}: {}", event.getOutboxId(), e.getMessage());
                event.setRetryCount(event.getRetryCount() + 1);
                if (event.getRetryCount() >= 5) {
                    event.setStatus("FAILED");
                }
                outboxRepository.save(event);
            }
        }
    }
}
