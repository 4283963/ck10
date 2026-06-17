package com.ck10.inventory.service;

import com.ck10.inventory.entity.ToolInventory;
import com.ck10.inventory.exception.InsufficientInventoryException;
import com.ck10.inventory.repository.ToolInventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ToolInventoryService {

    private final ToolInventoryRepository toolInventoryRepository;

    public List<ToolInventory> getAllInventory() {
        return toolInventoryRepository.findAll();
    }

    public Optional<ToolInventory> getInventoryById(Long id) {
        return toolInventoryRepository.findById(id);
    }

    public Optional<ToolInventory> getInventoryByModel(String toolModel) {
        return toolInventoryRepository.findByToolModel(toolModel);
    }

    @Transactional
    public ToolInventory createInventory(ToolInventory inventory) {
        if (toolInventoryRepository.existsByToolModel(inventory.getToolModel())) {
            throw new RuntimeException("刀具型号已存在: " + inventory.getToolModel());
        }
        return toolInventoryRepository.save(inventory);
    }

    @Transactional
    public ToolInventory updateInventory(Long id, ToolInventory inventory) {
        ToolInventory existing = toolInventoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("库存记录不存在: " + id));
        
        existing.setToolName(inventory.getToolName());
        existing.setTotalQuantity(inventory.getTotalQuantity());
        existing.setReservedQuantity(inventory.getReservedQuantity());
        existing.setMinStockLevel(inventory.getMinStockLevel());
        existing.setUnit(inventory.getUnit());
        existing.setSpecification(inventory.getSpecification());
        existing.setDescription(inventory.getDescription());
        
        return toolInventoryRepository.save(existing);
    }

    @Transactional
    public boolean tryReserveStock(String toolModel, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("预扣数量必须大于0");
        }

        int updated = toolInventoryRepository.reserveStock(toolModel, quantity);
        boolean success = updated > 0;
        
        if (success) {
            log.info("库存预扣成功: 型号 {}, 数量 {}", toolModel, quantity);
        } else {
            ToolInventory inv = toolInventoryRepository.findByToolModel(toolModel).orElse(null);
            int available = inv != null ? inv.getTotalQuantity() - inv.getReservedQuantity() : 0;
            log.warn("库存预扣失败: 型号 {}, 需要 {}, 可用 {}", toolModel, quantity, available);
        }
        
        return success;
    }

    @Transactional
    public void reserveStock(String toolModel, int quantity) {
        if (!tryReserveStock(toolModel, quantity)) {
            ToolInventory inv = toolInventoryRepository.findByToolModel(toolModel).orElse(null);
            int available = inv != null ? inv.getTotalQuantity() - inv.getReservedQuantity() : 0;
            throw new InsufficientInventoryException(toolModel, quantity, available);
        }
    }

    @Transactional
    public boolean tryReleaseReservedStock(String toolModel, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("释放数量必须大于0");
        }

        int updated = toolInventoryRepository.releaseReservedStock(toolModel, quantity);
        boolean success = updated > 0;
        
        if (success) {
            log.info("预扣库存释放: 型号 {}, 数量 {}", toolModel, quantity);
        } else {
            ToolInventory inv = toolInventoryRepository.findByToolModel(toolModel).orElse(null);
            int reserved = inv != null ? inv.getReservedQuantity() : 0;
            log.warn("预扣库存释放失败: 型号 {}, 释放 {}, 当前预扣 {}", toolModel, quantity, reserved);
        }
        
        return success;
    }

    @Transactional
    public void releaseReservedStock(String toolModel, int quantity) {
        if (!tryReleaseReservedStock(toolModel, quantity)) {
            throw new RuntimeException("预扣库存不足，无法释放");
        }
    }

    @Transactional
    public boolean tryConsumeStock(String toolModel, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("消耗数量必须大于0");
        }

        int updated = toolInventoryRepository.consumeStock(toolModel, quantity);
        boolean success = updated > 0;
        
        if (success) {
            log.info("库存消耗完成: 型号 {}, 数量 {}", toolModel, quantity);
        } else {
            ToolInventory inv = toolInventoryRepository.findByToolModel(toolModel).orElse(null);
            int reserved = inv != null ? inv.getReservedQuantity() : 0;
            log.warn("库存消耗失败: 型号 {}, 消耗 {}, 预扣 {}", toolModel, quantity, reserved);
        }
        
        return success;
    }

    @Transactional
    public void consumeStock(String toolModel, int quantity) {
        if (!tryConsumeStock(toolModel, quantity)) {
            throw new RuntimeException("预扣库存不足，无法消耗");
        }
    }

    @Transactional
    public void addStock(String toolModel, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("补货数量必须大于0");
        }

        int updated = toolInventoryRepository.addStock(toolModel, quantity);
        
        if (updated == 0) {
            throw new RuntimeException("刀具型号不存在: " + toolModel);
        }
        
        ToolInventory inv = toolInventoryRepository.findByToolModel(toolModel).orElse(null);
        int total = inv != null ? inv.getTotalQuantity() : 0;
        log.info("库存补充: 型号 {}, 数量 {}, 总库存 {}", toolModel, quantity, total);
    }

    public boolean isLowStock(String toolModel) {
        return toolInventoryRepository.findByToolModel(toolModel)
                .map(inv -> {
                    int available = inv.getTotalQuantity() - inv.getReservedQuantity();
                    return available <= inv.getMinStockLevel();
                })
                .orElse(false);
    }

    public int getAvailableQuantity(String toolModel) {
        return toolInventoryRepository.findByToolModel(toolModel)
                .map(inv -> inv.getTotalQuantity() - inv.getReservedQuantity())
                .orElse(0);
    }
}
