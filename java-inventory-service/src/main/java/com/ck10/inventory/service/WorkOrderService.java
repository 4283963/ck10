package com.ck10.inventory.service;

import com.ck10.inventory.dto.ToolLifeAlertRequest;
import com.ck10.inventory.entity.WorkOrder;
import com.ck10.inventory.repository.WorkOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkOrderService {

    private final WorkOrderRepository workOrderRepository;
    private final ToolInventoryService toolInventoryService;

    private final AtomicInteger orderCounter = new AtomicInteger(0);

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

    @Transactional
    public WorkOrder processToolLifeAlert(ToolLifeAlertRequest request) {
        log.info("收到刀具寿命预警: 刀具ID={}, 型号={}, 剩余寿命={}%, 阈值={}%",
                request.getToolId(), request.getToolModel(),
                request.getRemainingLife(), request.getThreshold());

        boolean exists = workOrderRepository.existsByToolIdAndStatusAndOrderType(
                request.getToolId(),
                WorkOrder.OrderStatus.PENDING,
                WorkOrder.OrderType.TOOL_REPLACEMENT
        );

        if (exists) {
            log.warn("刀具 {} 已有待处理的更换工单，跳过重复创建", request.getToolId());
            throw new RuntimeException("该刀具已有待处理的更换工单");
        }

        boolean reserved = toolInventoryService.reserveStock(request.getToolModel(), 1);
        if (!reserved) {
            log.error("库存预扣失败，无法创建工单: 型号 {}", request.getToolModel());
            throw new RuntimeException("库存不足，无法预扣");
        }

        try {
            WorkOrder workOrder = new WorkOrder();
            workOrder.setOrderNo(generateOrderNo());
            workOrder.setToolId(request.getToolId());
            workOrder.setToolModel(request.getToolModel());
            workOrder.setOrderType(WorkOrder.OrderType.TOOL_REPLACEMENT);
            workOrder.setStatus(WorkOrder.OrderStatus.PENDING);
            
            if (request.getRemainingLife() < 5) {
                workOrder.setPriority(WorkOrder.Priority.URGENT);
            } else if (request.getRemainingLife() < 10) {
                workOrder.setPriority(WorkOrder.Priority.HIGH);
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

            return saved;
        } catch (Exception e) {
            log.error("创建工单失败，回滚库存预扣: {}", e.getMessage());
            toolInventoryService.releaseReservedStock(request.getToolModel(), 1);
            throw e;
        }
    }

    @Transactional
    public WorkOrder completeOrder(Long id, String operator) {
        WorkOrder order = workOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("工单不存在: " + id));

        if (order.getStatus() == WorkOrder.OrderStatus.COMPLETED) {
            throw new RuntimeException("工单已完成");
        }

        boolean consumed = toolInventoryService.consumeStock(order.getToolModel(), order.getQuantity());
        if (!consumed) {
            throw new RuntimeException("库存消耗失败");
        }

        order.setStatus(WorkOrder.OrderStatus.COMPLETED);
        order.setOperator(operator);
        order.setCompletedAt(LocalDateTime.now());

        return workOrderRepository.save(order);
    }

    @Transactional
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

    @Transactional
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
