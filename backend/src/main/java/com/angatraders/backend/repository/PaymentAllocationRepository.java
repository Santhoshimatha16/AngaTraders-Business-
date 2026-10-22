
package com.angatraders.backend.repository;

import com.angatraders.backend.entity.PaymentAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentAllocationRepository
        extends JpaRepository<PaymentAllocation, Long> {

    List<PaymentAllocation> findByPaymentId(Long paymentId);

    List<PaymentAllocation> findByPurchaseId(Long purchaseId);

    List<PaymentAllocation> findBySaleId(Long saleId);
}
