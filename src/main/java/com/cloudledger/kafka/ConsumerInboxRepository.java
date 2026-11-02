package com.cloudledger.kafka;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConsumerInboxRepository extends JpaRepository<ConsumerInbox, ConsumerInbox.InboxId> {
}
