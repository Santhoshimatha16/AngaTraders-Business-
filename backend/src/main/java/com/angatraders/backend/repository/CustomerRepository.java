
package com.angatraders.backend.repository;

import com.angatraders.backend.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    List<Customer> findByCustomerNameContainingIgnoreCase(String customerName);

    List<Customer> findByPhoneContaining(String phone);

    List<Customer> findByStatus(String status);
}
