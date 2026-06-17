package com.ck10.inventory.exception;

public class InsufficientInventoryException extends RuntimeException {

    private final String toolModel;
    private final int requiredQuantity;
    private final int availableQuantity;

    public InsufficientInventoryException(String toolModel, int requiredQuantity, int availableQuantity) {
        super(String.format("库存不足: 型号=%s, 需要=%d, 可用=%d", toolModel, requiredQuantity, availableQuantity));
        this.toolModel = toolModel;
        this.requiredQuantity = requiredQuantity;
        this.availableQuantity = availableQuantity;
    }

    public String getToolModel() {
        return toolModel;
    }

    public int getRequiredQuantity() {
        return requiredQuantity;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }
}
