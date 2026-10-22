package com.angatraders.backend.dto;

import java.math.BigDecimal;

public class PaymentAllocationRequest {

    private Long purchaseId;
    private Long saleId;
    private BigDecimal amount;

    public PaymentAllocationRequest() {
    }

    public Long getPurchaseId() {
        return purchaseId;
    }

    public void setPurchaseId(Long purchaseId) {
        this.purchaseId = purchaseId;
    }

    public Long getSaleId() {
        return saleId;
    }

    public void setSaleId(Long saleId) {
        this.saleId = saleId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}