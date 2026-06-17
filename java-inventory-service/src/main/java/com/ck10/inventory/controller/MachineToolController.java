package com.ck10.inventory.controller;

import com.ck10.inventory.dto.ApiResponse;
import com.ck10.inventory.entity.MachineTool;
import com.ck10.inventory.service.MachineToolService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/machines")
@RequiredArgsConstructor
public class MachineToolController {

    private final MachineToolService machineToolService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<MachineTool>>> getAllMachines() {
        return ResponseEntity.ok(ApiResponse.success(machineToolService.getAllMachines()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MachineTool>> getMachineById(@PathVariable Long id) {
        return machineToolService.getMachineById(id)
                .map(machine -> ResponseEntity.ok(ApiResponse.success(machine)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/machine-id/{machineId}")
    public ResponseEntity<ApiResponse<MachineTool>> getMachineByMachineId(@PathVariable String machineId) {
        return machineToolService.getMachineByMachineId(machineId)
                .map(machine -> ResponseEntity.ok(ApiResponse.success(machine)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MachineTool>> createMachine(@RequestBody MachineTool machine) {
        MachineTool created = machineToolService.createMachine(machine);
        return ResponseEntity.ok(ApiResponse.success("创建成功", created));
    }

    @PostMapping("/{machineId}/emergency-stop")
    public ResponseEntity<ApiResponse<Boolean>> triggerEmergencyStop(
            @PathVariable String machineId,
            @RequestParam(required = false) String reason,
            @RequestParam(required = false, defaultValue = "manual") String triggeredBy) {
        boolean success = machineToolService.triggerEmergencyStop(
                machineId,
                reason != null ? reason : "手动触发紧急停机",
                triggeredBy
        );
        if (success) {
            return ResponseEntity.ok(ApiResponse.success("紧急停机已触发", true));
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.error("紧急停机触发失败"));
        }
    }

    @PostMapping("/{machineId}/resume")
    public ResponseEntity<ApiResponse<Boolean>> resumeMachine(@PathVariable String machineId) {
        boolean success = machineToolService.resumeMachine(machineId);
        if (success) {
            return ResponseEntity.ok(ApiResponse.success("机床已恢复运行", true));
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.error("机床恢复失败"));
        }
    }
}
