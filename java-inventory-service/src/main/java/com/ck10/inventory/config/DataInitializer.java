package com.ck10.inventory.config;

import com.ck10.inventory.entity.ToolInventory;
import com.ck10.inventory.service.ToolInventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final ToolInventoryService toolInventoryService;

    @Override
    public void run(String... args) {
        log.info("开始初始化刀具库存数据...");

        if (toolInventoryService.getAllInventory().isEmpty()) {
            createToolInventory("CNMG-120408", "外圆车刀片", 50, 5, "片", "80度菱形, 刀尖R0.8");
            createToolInventory("VNMG-160408", "外圆车刀片", 30, 3, "片", "35度菱形, 刀尖R0.8");
            createToolInventory("WNMG-080408", "外圆车刀片", 40, 4, "片", "80度六边形, 刀尖R0.8");
            createToolInventory("DNMG-150408", "外圆车刀片", 25, 3, "片", "55度菱形, 刀尖R0.8");
            createToolInventory("TNMG-160408", "外圆车刀片", 35, 4, "片", "60度三角形, 刀尖R0.8");
            createToolInventory("APMT-1604PDER", "铣刀片", 60, 8, "片", "方肩铣刀片");
            createToolInventory("RPMT-1204MO", "圆铣刀片", 45, 5, "片", "R6圆刀片");
            createToolInventory("SPMT-120408", "铣刀片", 30, 3, "片", "正方形铣刀片");
            
            log.info("刀具库存数据初始化完成，共导入 {} 种型号", toolInventoryService.getAllInventory().size());
        } else {
            log.info("库存数据已存在，跳过初始化");
        }
    }

    private void createToolInventory(String model, String name, int total, int minStock, String unit, String spec) {
        ToolInventory inventory = new ToolInventory();
        inventory.setToolModel(model);
        inventory.setToolName(name);
        inventory.setTotalQuantity(total);
        inventory.setReservedQuantity(0);
        inventory.setAvailableQuantity(total);
        inventory.setMinStockLevel(minStock);
        inventory.setUnit(unit);
        inventory.setSpecification(spec);
        inventory.setDescription("数控加工中心用高性能刀具");
        
        toolInventoryService.createInventory(inventory);
        log.info("创建库存: {} - {}, 数量: {}", model, name, total);
    }
}
