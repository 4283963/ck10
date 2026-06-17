package com.ck10.inventory.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "tool_inventory")
public class ToolInventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "tool_model", nullable = false, unique = true, length = 50)
    private String toolModel;

    @Column(name = "tool_name", length = 100)
    private String toolName;

    @Column(name = "total_quantity", nullable = false)
    private Integer totalQuantity;

    @Column(name = "reserved_quantity", nullable = false)
    private Integer reservedQuantity;

    @Formula("total_quantity - reserved_quantity")
    private Integer availableQuantity;

    @Column(name = "min_stock_level", nullable = false)
    private Integer minStockLevel;

    @Column(name = "unit", length = 20)
    private String unit;

    @Column(name = "specification", length = 200)
    private String specification;

    @Column(name = "description", length = 500)
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Transient
    public boolean isAvailable(int quantity) {
        return totalQuantity != null && reservedQuantity != null
                && (totalQuantity - reservedQuantity) >= quantity;
    }
}
