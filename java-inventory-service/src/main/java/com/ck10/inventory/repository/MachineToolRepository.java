package com.ck10.inventory.repository;

import com.ck10.inventory.entity.MachineTool;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface MachineToolRepository extends JpaRepository<MachineTool, Long> {

    Optional<MachineTool> findByMachineId(String machineId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM MachineTool m WHERE m.machineId = :machineId")
    Optional<MachineTool> findByMachineIdWithLock(@Param("machineId") String machineId);

    @Modifying
    @Query("UPDATE MachineTool m SET m.status = :status, m.statusUpdatedAt = :now, " +
           "m.emergencyReason = :reason WHERE m.machineId = :machineId")
    int updateStatus(@Param("machineId") String machineId,
                     @Param("status") MachineTool.MachineStatus status,
                     @Param("reason") String reason,
                     @Param("now") LocalDateTime now);

    boolean existsByMachineId(String machineId);
}
