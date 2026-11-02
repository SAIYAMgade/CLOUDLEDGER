package com.cloudledger.ledger.dto;

import com.cloudledger.ledger.JournalEntry;
import com.cloudledger.ledger.JournalStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class JournalEntryDTO {
    private UUID journalId;
    private UUID transactionId;
    private String journalNumber;

    @NotBlank(message = "Description is required")
    private String description;

    private JournalStatus status;

    @NotEmpty(message = "At least two lines (one debit, one credit) are required")
    @Valid
    private List<JournalLineDTO> lines;

    private Instant postedAt;
    private Instant createdAt;
    private String createdBy;

    public JournalEntryDTO() {}

    public static JournalEntryDTO from(JournalEntry entity) {
        JournalEntryDTO dto = new JournalEntryDTO();
        dto.setJournalId(entity.getJournalId());
        dto.setTransactionId(entity.getTransactionId());
        dto.setJournalNumber(entity.getJournalNumber());
        dto.setDescription(entity.getDescription());
        dto.setStatus(entity.getStatus());
        dto.setPostedAt(entity.getPostedAt());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setCreatedBy(entity.getCreatedBy());
        if (entity.getLines() != null) {
            dto.setLines(entity.getLines().stream()
                .map(line -> new JournalLineDTO(line.getAccountId(), line.getDirection(), line.getAmount(), line.getCurrency()))
                .collect(Collectors.toList()));
        }
        return dto;
    }

    public UUID getJournalId() { return journalId; }
    public void setJournalId(UUID journalId) { this.journalId = journalId; }

    public UUID getTransactionId() { return transactionId; }
    public void setTransactionId(UUID transactionId) { this.transactionId = transactionId; }

    public String getJournalNumber() { return journalNumber; }
    public void setJournalNumber(String journalNumber) { this.journalNumber = journalNumber; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public JournalStatus getStatus() { return status; }
    public void setStatus(JournalStatus status) { this.status = status; }

    public List<JournalLineDTO> getLines() { return lines; }
    public void setLines(List<JournalLineDTO> lines) { this.lines = lines; }

    public Instant getPostedAt() { return postedAt; }
    public void setPostedAt(Instant postedAt) { this.postedAt = postedAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
}
