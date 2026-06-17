package com.ck10.inventory.config;

import com.ck10.inventory.entity.MachineTool;
import com.ck10.inventory.entity.ToolInventory;
import com.ck10.inventory.service.MachineToolService;
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
    private final MachineToolService machineToolService;

    @Override
    public void run(String... args) {
        initToolInventory();
        initMachines();
    }

    private void initToolInventory() {
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

    private void initMachines() {
        log.info("开始初始化机床数据...");

        if (machineToolService.getAllMachines().isEmpty()) {
            createMachine("CNC-001", "CNC车削中心-01", "一车间", "CK6150");
            createMachine("CNC-002", "CNC车削中心-02", "一车间", "CK6150");
            createMachine("CNC-003", "CNC车削中心-03", "二车间", "CK6163");
            createMachine("CNC-004", "CNC铣削中心-01", "二车间", "VMC850");

            log.info("机床数据初始化完成，共导入 {} 台", machineToolService.getAllMachines().size());
        } else {
            log.info("机床数据已存在，跳过初始化");
        }
    }

    private void createToolInventory(String model, String name, int total, int minStock, String unit, String spec) {
        ToolInventory inventory = new ToolInventory();
        inventory.setToolModel(model);
        inventory.setToolName(name);
        inventory.setTotalQuantity(total);
        inventory.setReservedQuantity(0);
        inventory.setMinStockLevel(minStock);
        inventory.setUnit(unit);
        inventory.setSpecification(spec);
        inventory.setDescription("数控加工中心用高性能刀具");
        
        toolInventoryService.createInventory(inventory);
        log.info("创建库存: {} - {}, 数量: {}", model, name, total);
    }

    private void createMachine(String machineId, String name, String workshop, String model) {
        MachineTool machine = new MachineTool();
        machine.setMachineId(machineId);
        machine.setName(name);
        machine.setWorkshop(workshop);
        machine.setModel(model);
        machine.setStatus(MachineTool.MachineStatus.RUNNING);

        machineToolService.createMachine(machine);
        log.info("创建机床: {} - {}", machineId, name);
    }
}
