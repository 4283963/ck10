package com.ck10.inventory.repository;

import com.ck10.inventory.entity.WorkOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkOrderRepository extends JpaRepository<WorkOrder, Long> {
    Optional<WorkOrder> findByOrderNo(String orderNo);
    List<WorkOrder> findByToolId(String toolId);
    List<WorkOrder> findByStatus(WorkOrder.OrderStatus status);
    List<WorkOrder> findByOrderType(WorkOrder.OrderType orderType);
    boolean existsByToolIdAndStatusAndOrderType(String toolId, WorkOrder.OrderStatus status, WorkOrder.OrderType orderType);
}
