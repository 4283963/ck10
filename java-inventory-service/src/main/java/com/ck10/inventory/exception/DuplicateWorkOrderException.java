package com.ck10.inventory.exception;

public class DuplicateWorkOrderException extends RuntimeException {

    private final String toolId;

    public DuplicateWorkOrderException(String toolId) {
        super(String.format("刀具 %s 已有待处理的更换工单", toolId));
        this.toolId = toolId;
    }

    public String getToolId() {
        return toolId;
    }
}
