package com.cloudledger.identity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "roles")
public class Role {

    @Id
    @Column(name = "role_id", length = 32)
    private String roleId;

    @Column(nullable = false)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Role() {}

    public Role(String roleId, String description) {
        this.roleId = roleId;
        this.description = description;
    }

    public String getRoleId() { return roleId; }
    public void setRoleId(String roleId) { this.roleId = roleId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
