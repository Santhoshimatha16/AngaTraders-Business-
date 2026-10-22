
package com.angatraders.backend.repository;

import com.angatraders.backend.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByPaymentDate(LocalDate paymentDate);

    List<Payment> findByPaymentDateBetween(
            LocalDate startDate,
            LocalDate endDate
    );

    List<Payment> findByPaymentTypeIgnoreCase(String paymentType);

    List<Payment> findByCompanyId(Long companyId);

    List<Payment> findByCustomerId(Long customerId);
}
