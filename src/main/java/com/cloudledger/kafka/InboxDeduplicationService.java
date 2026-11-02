package com.cloudledger.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InboxDeduplicationService {

    private static final Logger log = LoggerFactory.getLogger(InboxDeduplicationService.class);

    private final ConsumerInboxRepository consumerInboxRepository;

    public InboxDeduplicationService(ConsumerInboxRepository consumerInboxRepository) {
        this.consumerInboxRepository = consumerInboxRepository;
    }

    /**
     * Checks if the message has already been processed by this consumer group.
     * Returns true if already processed (duplicate), false if new.
     */
    @Transactional
    public boolean isDuplicateAndMark(String messageId, String consumerGroup) {
        ConsumerInbox.InboxId id = new ConsumerInbox.InboxId(messageId, consumerGroup);
        if (consumerInboxRepository.existsById(id)) {
            log.info("Consumer inbox: duplicate message {} detected for consumer group {}", messageId, consumerGroup);
            return true;
        }

        consumerInboxRepository.save(new ConsumerInbox(messageId, consumerGroup, "PROCESSED"));
        return false;
    }
}
