package com.ck10.inventory.service;

import com.ck10.inventory.dto.ToolLifeAlertRequest;
import com.ck10.inventory.entity.WorkOrder;
import com.ck10.inventory.exception.DuplicateWorkOrderException;
import com.ck10.inventory.exception.InsufficientInventoryException;
import com.ck10.inventory.repository.WorkOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkOrderService {

    private final WorkOrderRepository workOrderRepository;
    private final ToolInventoryService toolInventoryService;
    private final MachineToolService machineToolService;

    private final AtomicInteger orderCounter = new AtomicInteger(0);
    private static final double CRITICAL_LIFE_THRESHOLD = 5.0;

    public List<WorkOrder> getAllOrders() {
        return workOrderRepository.findAll();
    }

    public Optional<WorkOrder> getOrderById(Long id) {
        return workOrderRepository.findById(id);
    }

    public Optional<WorkOrder> getOrderByNo(String orderNo) {
        return workOrderRepository.findByOrderNo(orderNo);
    }

    public List<WorkOrder> getOrdersByToolId(String toolId) {
        return workOrderRepository.findByToolId(toolId);
    }

    public List<WorkOrder> getOrdersByStatus(WorkOrder.OrderStatus status) {
        return workOrderRepository.findByStatus(status);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> processToolLifeAlert(ToolLifeAlertRequest request) {
        log.info("收到刀具寿命预警: 刀具ID={}, 型号={}, 剩余寿命={}%, 阈值={}%, 机床={}",
                request.getToolId(), request.getToolModel(),
                request.getRemainingLife(), request.getThreshold(), request.getMachineId());

        Map<String, Object> result = new HashMap<>();

        boolean reserved = toolInventoryService.tryReserveStock(request.getToolModel(), 1);

        if (!reserved) {
            log.warn("库存预扣失败: 型号 {} - 检查是否需要触发生产线熔断", request.getToolModel());

            boolean isCritical = request.getRemainingLife() != null
                    && request.getRemainingLife() < CRITICAL_LIFE_THRESHOLD;
            boolean hasMachineId = request.getMachineId() != null
                    && !request.getMachineId().isBlank();
            boolean emergencyStopTriggered = false;
            String emergencyStopReason = null;

            if (isCritical && hasMachineId) {
                log.error("【极度危险】刀具 {} 剩余寿命 {}% < 5%，且库存已耗尽！触发生产线熔断！",
                        request.getToolId(), request.getRemainingLife());

                emergencyStopReason = String.format("刀具型号 %s 库存已耗尽，当前刀具 %s 剩余寿命 %.2f%% < 5%%，系统自动触发紧急停机",
                        request.getToolModel(), request.getToolId(), request.getRemainingLife());

                emergencyStopTriggered = machineToolService.triggerEmergencyStopInNewTransaction(
                        request.getMachineId(), emergencyStopReason, "system_fuse");

                if (emergencyStopTriggered) {
                    log.warn("生产线熔断已触发，机床 {} 已紧急停机", request.getMachineId());
                } else {
                    log.error("生产线熔断触发失败！机床 {} 状态未更新", request.getMachineId());
                }
            }

            result.put("success", false);
            result.put("emergencyStopTriggered", emergencyStopTriggered);
            result.put("emergencyStopReason", emergencyStopReason);
            result.put("remainingLife", request.getRemainingLife());
            result.put("criticalThreshold", CRITICAL_LIFE_THRESHOLD);
            result.put("availableQuantity", toolInventoryService.getAvailableQuantity(request.getToolModel()));
            result.put("requiredQuantity", 1);
            result.put("message", "库存不足，无法创建工单" + (emergencyStopTriggered ? "，已触发紧急停机" : ""));
            return result;
        }

        log.debug("库存预扣成功: 型号 {}", request.getToolModel());

        boolean exists = workOrderRepository.existsByToolIdAndStatusAndOrderType(
                request.getToolId(),
                WorkOrder.OrderStatus.PENDING,
                WorkOrder.OrderType.TOOL_REPLACEMENT
        );
        if (exists) {
            log.warn("刀具 {} 已有待处理的更换工单，事务回滚", request.getToolId());
            toolInventoryService.releaseReservedStock(request.getToolModel(), 1);
            throw new DuplicateWorkOrderException(request.getToolId());
        }

        WorkOrder workOrder = new WorkOrder();
        workOrder.setOrderNo(generateOrderNo());
        workOrder.setToolId(request.getToolId());
        workOrder.setToolModel(request.getToolModel());
        workOrder.setMachineId(request.getMachineId());
        workOrder.setOrderType(WorkOrder.OrderType.TOOL_REPLACEMENT);
        workOrder.setStatus(WorkOrder.OrderStatus.PENDING);

        if (request.getRemainingLife() != null) {
            if (request.getRemainingLife() < 5) {
                workOrder.setPriority(WorkOrder.Priority.URGENT);
            } else if (request.getRemainingLife() < 10) {
                workOrder.setPriority(WorkOrder.Priority.HIGH);
            } else {
                workOrder.setPriority(WorkOrder.Priority.MEDIUM);
            }
        } else {
            workOrder.setPriority(WorkOrder.Priority.MEDIUM);
        }

        workOrder.setRemainingLife(request.getRemainingLife());
        workOrder.setThreshold(request.getThreshold());
        workOrder.setQuantity(1);
        workOrder.setRemark("刀具寿命低于阈值，自动生成更换工单");

        WorkOrder saved = workOrderRepository.save(workOrder);
        log.info("刀具更换工单创建成功: 工单号={}, 刀具ID={}", saved.getOrderNo(), saved.getToolId());

        if (toolInventoryService.isLowStock(request.getToolModel())) {
            log.warn("刀具型号 {} 库存已低于安全线，建议补货", request.getToolModel());
        }

        result.put("success", true);
        result.put("workOrder", saved);
        result.put("emergencyStopTriggered", false);
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public WorkOrder completeOrder(Long id, String operator) {
        WorkOrder order = workOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("工单不存在: " + id));

        if (order.getStatus() == WorkOrder.OrderStatus.COMPLETED) {
            throw new RuntimeException("工单已完成");
        }

        toolInventoryService.consumeStock(order.getToolModel(), order.getQuantity());

        order.setStatus(WorkOrder.OrderStatus.COMPLETED);
        order.setOperator(operator);
        order.setCompletedAt(LocalDateTime.now());

        return workOrderRepository.save(order);
    }

    @Transactional(rollbackFor = Exception.class)
    public WorkOrder cancelOrder(Long id, String reason) {
        WorkOrder order = workOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("工单不存在: " + id));

        if (order.getStatus() == WorkOrder.OrderStatus.COMPLETED) {
            throw new RuntimeException("已完成的工单无法取消");
        }

        toolInventoryService.releaseReservedStock(order.getToolModel(), order.getQuantity());

        order.setStatus(WorkOrder.OrderStatus.CANCELLED);
        order.setRemark(order.getRemark() + " | 取消原因: " + reason);

        return workOrderRepository.save(order);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public WorkOrder updateOrderStatus(Long id, WorkOrder.OrderStatus status) {
        WorkOrder order = workOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("工单不存在: " + id));

        order.setStatus(status);
        return workOrderRepository.save(order);
    }

    private String generateOrderNo() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int seq = orderCounter.incrementAndGet();
        return String.format("WO%s%06d", datePart, seq);
    }
}
