package com.ck10.inventory.controller;

import com.ck10.inventory.dto.ApiResponse;
import com.ck10.inventory.entity.ToolInventory;
import com.ck10.inventory.service.ToolInventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class ToolInventoryController {

    private final ToolInventoryService toolInventoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ToolInventory>>> getAllInventory() {
        return ResponseEntity.ok(ApiResponse.success(toolInventoryService.getAllInventory()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ToolInventory>> getInventoryById(@PathVariable Long id) {
        return toolInventoryService.getInventoryById(id)
                .map(inv -> ResponseEntity.ok(ApiResponse.success(inv)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/model/{toolModel}")
    public ResponseEntity<ApiResponse<ToolInventory>> getInventoryByModel(@PathVariable String toolModel) {
        return toolInventoryService.getInventoryByModel(toolModel)
                .map(inv -> ResponseEntity.ok(ApiResponse.success(inv)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ToolInventory>> createInventory(@RequestBody ToolInventory inventory) {
        ToolInventory created = toolInventoryService.createInventory(inventory);
        return ResponseEntity.ok(ApiResponse.success("创建成功", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ToolInventory>> updateInventory(
            @PathVariable Long id,
            @RequestBody ToolInventory inventory) {
        ToolInventory updated = toolInventoryService.updateInventory(id, inventory);
        return ResponseEntity.ok(ApiResponse.success("更新成功", updated));
    }

    @PostMapping("/{toolModel}/reserve")
    public ResponseEntity<ApiResponse<Void>> reserveStock(
            @PathVariable String toolModel,
            @RequestParam(defaultValue = "1") int quantity) {
        toolInventoryService.reserveStock(toolModel, quantity);
        return ResponseEntity.ok(ApiResponse.success("预扣成功", null));
    }

    @PostMapping("/{toolModel}/release")
    public ResponseEntity<ApiResponse<Void>> releaseReservedStock(
            @PathVariable String toolModel,
            @RequestParam(defaultValue = "1") int quantity) {
        toolInventoryService.releaseReservedStock(toolModel, quantity);
        return ResponseEntity.ok(ApiResponse.success("释放成功", null));
    }

    @PostMapping("/{toolModel}/consume")
    public ResponseEntity<ApiResponse<Void>> consumeStock(
            @PathVariable String toolModel,
            @RequestParam(defaultValue = "1") int quantity) {
        toolInventoryService.consumeStock(toolModel, quantity);
        return ResponseEntity.ok(ApiResponse.success("消耗成功", null));
    }

    @PostMapping("/{toolModel}/add")
    public ResponseEntity<ApiResponse<Void>> addStock(
            @PathVariable String toolModel,
            @RequestParam(defaultValue = "1") int quantity) {
        toolInventoryService.addStock(toolModel, quantity);
        return ResponseEntity.ok(ApiResponse.success("补货成功", null));
    }
}
