package com.ck10.inventory.service;

import com.ck10.inventory.entity.ToolInventory;
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
        inventory.setAvailableQuantity(inventory.getTotalQuantity() - inventory.getReservedQuantity());
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
    public boolean reserveStock(String toolModel, int quantity) {
        ToolInventory inventory = toolInventoryRepository.findByToolModel(toolModel)
                .orElseThrow(() -> new RuntimeException("刀具型号不存在: " + toolModel));
        
        int available = inventory.getTotalQuantity() - inventory.getReservedQuantity();
        if (available < quantity) {
            log.warn("库存不足: 型号 {}, 可用 {}, 需要 {}", toolModel, available, quantity);
            return false;
        }
        
        inventory.setReservedQuantity(inventory.getReservedQuantity() + quantity);
        toolInventoryRepository.save(inventory);
        
        log.info("库存预扣成功: 型号 {}, 数量 {}, 剩余可用 {}", 
                toolModel, quantity, inventory.getAvailableQuantity());
        return true;
    }

    @Transactional
    public boolean releaseReservedStock(String toolModel, int quantity) {
        ToolInventory inventory = toolInventoryRepository.findByToolModel(toolModel)
                .orElseThrow(() -> new RuntimeException("刀具型号不存在: " + toolModel));
        
        if (inventory.getReservedQuantity() < quantity) {
            log.warn("预扣库存不足: 型号 {}, 预扣 {}, 释放 {}", 
                    toolModel, inventory.getReservedQuantity(), quantity);
            return false;
        }
        
        inventory.setReservedQuantity(inventory.getReservedQuantity() - quantity);
        toolInventoryRepository.save(inventory);
        
        log.info("预扣库存释放: 型号 {}, 数量 {}", toolModel, quantity);
        return true;
    }

    @Transactional
    public boolean consumeStock(String toolModel, int quantity) {
        ToolInventory inventory = toolInventoryRepository.findByToolModel(toolModel)
                .orElseThrow(() -> new RuntimeException("刀具型号不存在: " + toolModel));
        
        if (inventory.getReservedQuantity() < quantity) {
            log.warn("预扣库存不足，无法消耗: 型号 {}, 预扣 {}, 消耗 {}", 
                    toolModel, inventory.getReservedQuantity(), quantity);
            return false;
        }
        
        inventory.setTotalQuantity(inventory.getTotalQuantity() - quantity);
        inventory.setReservedQuantity(inventory.getReservedQuantity() - quantity);
        toolInventoryRepository.save(inventory);
        
        log.info("库存消耗完成: 型号 {}, 数量 {}, 剩余库存 {}", 
                toolModel, quantity, inventory.getTotalQuantity());
        return true;
    }

    @Transactional
    public void addStock(String toolModel, int quantity) {
        ToolInventory inventory = toolInventoryRepository.findByToolModel(toolModel)
                .orElseThrow(() -> new RuntimeException("刀具型号不存在: " + toolModel));
        
        inventory.setTotalQuantity(inventory.getTotalQuantity() + quantity);
        toolInventoryRepository.save(inventory);
        
        log.info("库存补充: 型号 {}, 数量 {}, 总库存 {}", 
                toolModel, quantity, inventory.getTotalQuantity());
    }

    public boolean isLowStock(String toolModel) {
        return toolInventoryRepository.findByToolModel(toolModel)
                .map(inv -> inv.getAvailableQuantity() <= inv.getMinStockLevel())
                .orElse(false);
    }
}
