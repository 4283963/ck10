package com.ck10.inventory.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "machine_tools")
public class MachineTool {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "machine_id", nullable = false, unique = true, length = 50)
    private String machineId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "status", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private MachineStatus status;

    @Column(name = "workshop", length = 50)
    private String workshop;

    @Column(name = "model", length = 100)
    private String model;

    @Column(name = "emergency_reason", length = 500)
    private String emergencyReason;

    @Column(name = "status_updated_at")
    private LocalDateTime statusUpdatedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum MachineStatus {
        RUNNING,
        IDLE,
        MAINTENANCE,
        EMERGENCY_STOP,
        OFFLINE
    }
}
