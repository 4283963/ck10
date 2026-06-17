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
import java.util.Map;

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
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleToolLifeAlert(@RequestBody ToolLifeAlertRequest request) {
        log.info("收到刀具寿命预警通知: toolId={}, model={}, remainingLife={}%, machineId={}",
                request.getToolId(), request.getToolModel(), request.getRemainingLife(), request.getMachineId());

        Map<String, Object> result = workOrderService.processToolLifeAlert(request);

        boolean success = Boolean.TRUE.equals(result.get("success"));
        boolean emergencyStopTriggered = Boolean.TRUE.equals(result.get("emergencyStopTriggered"));

        if (success) {
            return ResponseEntity.ok(ApiResponse.success("工单创建成功", result));
        } else if (emergencyStopTriggered) {
            log.warn("库存不足且刀具危险，已触发生产线熔断: {}", result.get("emergencyStopReason"));
            return ResponseEntity.ok(ApiResponse.success("库存不足，已触发生产线紧急停机熔断", result));
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.error((String) result.get("message")));
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
