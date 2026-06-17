package com.ck10.inventory.controller;

import com.ck10.inventory.dto.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> healthCheck() {
        Map<String, Object> data = new HashMap<>();
        data.put("status", "healthy");
        data.put("service", "inventory-service");
        data.put("timestamp", LocalDateTime.now().toString());
        return ApiResponse.success(data);
    }
}
