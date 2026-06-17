package com.ck10.inventory.controller;

import com.ck10.inventory.dto.ApiResponse;
import com.ck10.inventory.dto.ToolLifeAlertRequest;
import com.ck10.inventory.entity.WorkOrder;
import com.ck10.inventory.service.WorkOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/work-orders")
@RequiredArgsConstructor
public class WorkOrderController {

    private final WorkOrderService workOrderService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<WorkOrder>>> getAllOrders() {
        return ResponseEntity.ok(ApiResponse.success(workOrderService.getAllOrders()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<WorkOrder>> getOrderById(@PathVariable Long id) {
        return workOrderService.getOrderById(id)
                .map(order -> ResponseEntity.ok(ApiResponse.success(order)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/no/{orderNo}")
    public ResponseEntity<ApiResponse<WorkOrder>> getOrderByNo(@PathVariable String orderNo) {
        return workOrderService.getOrderByNo(orderNo)
                .map(order -> ResponseEntity.ok(ApiResponse.success(order)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/tool/{toolId}")
    public ResponseEntity<ApiResponse<List<WorkOrder>>> getOrdersByToolId(@PathVariable String toolId) {
        return ResponseEntity.ok(ApiResponse.success(workOrderService.getOrdersByToolId(toolId)));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<WorkOrder>>> getOrdersByStatus(@PathVariable WorkOrder.OrderStatus status) {
        return ResponseEntity.ok(ApiResponse.success(workOrderService.getOrdersByStatus(status)));
    }

    @PostMapping("/life-alert")
    public ResponseEntity<ApiResponse<WorkOrder>> handleToolLifeAlert(@RequestBody ToolLifeAlertRequest request) {
        log.info("收到刀具寿命预警通知: toolId={}, model={}, remainingLife={}%",
                request.getToolId(), request.getToolModel(), request.getRemainingLife());
        
        try {
            WorkOrder workOrder = workOrderService.processToolLifeAlert(request);
            return ResponseEntity.ok(ApiResponse.success("工单创建成功", workOrder));
        } catch (RuntimeException e) {
            log.error("处理刀具寿命预警失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<WorkOrder>> completeOrder(
            @PathVariable Long id,
            @RequestParam(required = false) String operator) {
        try {
            WorkOrder order = workOrderService.completeOrder(id, operator != null ? operator : "system");
            return ResponseEntity.ok(ApiResponse.success("工单已完成", order));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<WorkOrder>> cancelOrder(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "手动取消") String reason) {
        try {
            WorkOrder order = workOrderService.cancelOrder(id, reason);
            return ResponseEntity.ok(ApiResponse.success("工单已取消", order));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<WorkOrder>> updateOrderStatus(
            @PathVariable Long id,
            @RequestParam WorkOrder.OrderStatus status) {
        try {
            WorkOrder order = workOrderService.updateOrderStatus(id, status);
            return ResponseEntity.ok(ApiResponse.success("状态已更新", order));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
