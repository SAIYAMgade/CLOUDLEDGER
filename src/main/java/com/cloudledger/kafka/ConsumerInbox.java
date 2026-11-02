package com.cloudledger.kafka;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "consumer_inbox")
@IdClass(ConsumerInbox.InboxId.class)
public class ConsumerInbox {

    @Id
    @Column(name = "message_id", length = 128)
    private String messageId;

    @Id
    @Column(name = "consumer_group", length = 64)
    private String consumerGroup;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "processed_at", nullable = false, updatable = false)
    private Instant processedAt = Instant.now();

    public ConsumerInbox() {}

    public ConsumerInbox(String messageId, String consumerGroup, String status) {
        this.messageId = messageId;
        this.consumerGroup = consumerGroup;
        this.status = status;
        this.processedAt = Instant.now();
    }

    public static class InboxId implements Serializable {
        private String messageId;
        private String consumerGroup;

        public InboxId() {}
        public InboxId(String messageId, String consumerGroup) {
            this.messageId = messageId;
            this.consumerGroup = consumerGroup;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            InboxId inboxId = (InboxId) o;
            return Objects.equals(messageId, inboxId.messageId) && Objects.equals(consumerGroup, inboxId.consumerGroup);
        }

        @Override
        public int hashCode() {
            return Objects.hash(messageId, consumerGroup);
        }
    }

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public String getConsumerGroup() { return consumerGroup; }
    public void setConsumerGroup(String consumerGroup) { this.consumerGroup = consumerGroup; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getProcessedAt() { return processedAt; }
    public void setProcessedAt(Instant processedAt) { this.processedAt = processedAt; }
}
