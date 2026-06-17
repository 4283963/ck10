package com.ck10.inventory.repository;

import com.ck10.inventory.entity.ToolInventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ToolInventoryRepository extends JpaRepository<ToolInventory, Long> {

    Optional<ToolInventory> findByToolModel(String toolModel);

    boolean existsByToolModel(String toolModel);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM ToolInventory t WHERE t.toolModel = :toolModel")
    Optional<ToolInventory> findByToolModelWithLock(@Param("toolModel") String toolModel);

    @Modifying
    @Query("UPDATE ToolInventory t SET t.reservedQuantity = t.reservedQuantity + :quantity " +
           "WHERE t.toolModel = :toolModel AND (t.totalQuantity - t.reservedQuantity) >= :quantity")
    int reserveStock(@Param("toolModel") String toolModel, @Param("quantity") int quantity);

    @Modifying
    @Query("UPDATE ToolInventory t SET t.reservedQuantity = t.reservedQuantity - :quantity " +
           "WHERE t.toolModel = :toolModel AND t.reservedQuantity >= :quantity")
    int releaseReservedStock(@Param("toolModel") String toolModel, @Param("quantity") int quantity);

    @Modifying
    @Query("UPDATE ToolInventory t SET t.totalQuantity = t.totalQuantity - :quantity, " +
           "t.reservedQuantity = t.reservedQuantity - :quantity " +
           "WHERE t.toolModel = :toolModel AND t.reservedQuantity >= :quantity")
    int consumeStock(@Param("toolModel") String toolModel, @Param("quantity") int quantity);

    @Modifying
    @Query("UPDATE ToolInventory t SET t.totalQuantity = t.totalQuantity + :quantity " +
           "WHERE t.toolModel = :toolModel")
    int addStock(@Param("toolModel") String toolModel, @Param("quantity") int quantity);
}
