package com.ck10.inventory.repository;

import com.ck10.inventory.entity.ToolInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ToolInventoryRepository extends JpaRepository<ToolInventory, Long> {
    Optional<ToolInventory> findByToolModel(String toolModel);
    boolean existsByToolModel(String toolModel);
}
