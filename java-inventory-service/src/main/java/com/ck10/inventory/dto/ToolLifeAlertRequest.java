package com.ck10.inventory.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ToolLifeAlertRequest {

    @JsonProperty("toolId")
    private String toolId;

    @JsonProperty("toolModel")
    private String toolModel;

    @JsonProperty("remainingLife")
    private Double remainingLife;

    @JsonProperty("threshold")
    private Double threshold;

    @JsonProperty("timestamp")
    private String timestamp;
}
