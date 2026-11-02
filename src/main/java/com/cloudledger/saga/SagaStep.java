package com.cloudledger.saga;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "saga_steps")
public class SagaStep {

    @Id
    @Column(name = "step_id")
    private UUID stepId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "saga_id", nullable = false)
    @JsonIgnore
    private SagaInstance sagaInstance;

    @Column(name = "step_name", nullable = false, length = 64)
    private String stepName;

    @Column(nullable = false, length = 32)
    private String status; // STARTED, COMPLETED, FAILED, COMPENSATED

    @Column(name = "step_order", nullable = false)
    private int stepOrder;

    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt = Instant.now();

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    public SagaStep() {
        this.stepId = UUID.randomUUID();
    }

    public SagaStep(String stepName, int stepOrder) {
        this.stepId = UUID.randomUUID();
        this.stepName = stepName;
        this.stepOrder = stepOrder;
        this.status = "STARTED";
        this.startedAt = Instant.now();
    }

    public UUID getStepId() { return stepId; }
    public void setStepId(UUID stepId) { this.stepId = stepId; }

    public SagaInstance getSagaInstance() { return sagaInstance; }
    public void setSagaInstance(SagaInstance sagaInstance) { this.sagaInstance = sagaInstance; }

    public String getStepName() { return stepName; }
    public void setStepName(String stepName) { this.stepName = stepName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getStepOrder() { return stepOrder; }
    public void setStepOrder(int stepOrder) { this.stepOrder = stepOrder; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
