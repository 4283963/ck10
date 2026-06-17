package com.ck10.inventory.service;

import com.ck10.inventory.config.PythonServiceConfig;
import com.ck10.inventory.entity.MachineTool;
import com.ck10.inventory.repository.MachineToolRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MachineToolService {

    private final MachineToolRepository machineToolRepository;
    private final PythonServiceConfig pythonServiceConfig;
    private final RestTemplate restTemplate;

    public List<MachineTool> getAllMachines() {
        return machineToolRepository.findAll();
    }

    public Optional<MachineTool> getMachineById(Long id) {
        return machineToolRepository.findById(id);
    }

    public Optional<MachineTool> getMachineByMachineId(String machineId) {
        return machineToolRepository.findByMachineId(machineId);
    }

    @Transactional
    public MachineTool createMachine(MachineTool machine) {
        if (machineToolRepository.existsByMachineId(machine.getMachineId())) {
            throw new RuntimeException("机床ID已存在: " + machine.getMachineId());
        }
        machine.setStatusUpdatedAt(LocalDateTime.now());
        return machineToolRepository.save(machine);
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean triggerEmergencyStop(String machineId, String reason, String triggeredBy) {
        log.error("【生产线熔断】触发机床 {} 紧急停机，原因: {}, 触发者: {}", machineId, reason, triggeredBy);

        int updated = machineToolRepository.updateStatus(
                machineId,
                MachineTool.MachineStatus.EMERGENCY_STOP,
                reason,
                LocalDateTime.now()
        );

        if (updated == 0) {
            log.error("机床状态更新失败: {}", machineId);
            return false;
        }

        boolean pythonNotified = notifyPythonEmergencyStop(machineId, reason, triggeredBy);
        if (!pythonNotified) {
            log.error("通知 Python 服务紧急停机失败，机床 {} 的本地状态已更新但远端未同步", machineId);
        }

        log.error("【紧急停机完成】机床 {} 已熔断停机", machineId);
        return true;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public boolean triggerEmergencyStopInNewTransaction(String machineId, String reason, String triggeredBy) {
        return triggerEmergencyStop(machineId, reason, triggeredBy);
    }

    private boolean notifyPythonEmergencyStop(String machineId, String reason, String triggeredBy) {
        try {
            String url = pythonServiceConfig.getBaseUrl() + "/api/machines/" + machineId + "/emergency-stop";
            
            Map<String, String> payload = new HashMap<>();
            payload.put("reason", reason);
            payload.put("triggeredBy", triggeredBy);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, payload, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                if (Boolean.TRUE.equals(body.get("success"))) {
                    log.info("Python 服务已确认机床 {} 紧急停机", machineId);
                    return true;
                }
            }
            log.warn("Python 服务返回非成功响应: {}", response);
            return false;
        } catch (Exception e) {
            log.error("通知 Python 服务紧急停机异常: {}", e.getMessage());
            return false;
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean resumeMachine(String machineId) {
        int updated = machineToolRepository.updateStatus(
                machineId,
                MachineTool.MachineStatus.RUNNING,
                null,
                LocalDateTime.now()
        );

        if (updated == 0) {
            return false;
        }

        try {
            String url = pythonServiceConfig.getBaseUrl() + "/api/machines/" + machineId + "/resume";
            restTemplate.postForEntity(url, null, Map.class);
        } catch (Exception e) {
            log.warn("通知 Python 服务恢复机床失败: {}", e.getMessage());
        }

        log.info("机床 {} 已恢复运行", machineId);
        return true;
    }

    public boolean isEmergencyStopped(String machineId) {
        return machineToolRepository.findByMachineId(machineId)
                .map(m -> m.getStatus() == MachineTool.MachineStatus.EMERGENCY_STOP)
                .orElse(false);
    }
}
