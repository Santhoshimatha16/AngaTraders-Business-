
package com.angatraders.backend.service;

import com.angatraders.backend.entity.Customer;
import com.angatraders.backend.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    public Customer getCustomerById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Customer not found with ID: " + id));
    }

    public List<Customer> searchByName(String name) {
        return customerRepository.findByCustomerNameContainingIgnoreCase(name);
    }

    public List<Customer> searchByPhone(String phone) {
        return customerRepository.findByPhoneContaining(phone);
    }

    public List<Customer> getCustomersByStatus(String status) {
        return customerRepository.findByStatus(status);
    }

    @Transactional
    public Customer createCustomer(Customer customer) {
        validateCustomer(customer);

        LocalDateTime now = LocalDateTime.now();
        customer.setCreatedAt(now);
        customer.setUpdatedAt(now);

        if (customer.getStatus() == null || customer.getStatus().isBlank()) {
            customer.setStatus("Active");
        }

        return customerRepository.save(customer);
    }

    @Transactional
    public Customer updateCustomer(Long id, Customer updatedCustomer) {
        Customer existing = getCustomerById(id);

        validateCustomer(updatedCustomer);

        existing.setCustomerName(updatedCustomer.getCustomerName().trim());
        existing.setPhone(updatedCustomer.getPhone());
        existing.setAddress(updatedCustomer.getAddress());
        existing.setLocation(updatedCustomer.getLocation());
        existing.setNotes(updatedCustomer.getNotes());

        if (updatedCustomer.getStatus() != null
                && !updatedCustomer.getStatus().isBlank()) {
            existing.setStatus(updatedCustomer.getStatus());
        }

        existing.setUpdatedAt(LocalDateTime.now());

        return customerRepository.save(existing);
    }

    @Transactional
    public void deleteCustomer(Long id) {
        Customer customer = getCustomerById(id);
        customerRepository.delete(customer);
    }

    private void validateCustomer(Customer customer) {
        if (customer.getCustomerName() == null
                || customer.getCustomerName().isBlank()) {
            throw new IllegalArgumentException("Customer name is required");
        }

        customer.setCustomerName(customer.getCustomerName().trim());

        if (customer.getPhone() != null
                && !customer.getPhone().isBlank()
                && !customer.getPhone().matches("[0-9+() -]{7,20}")) {
            throw new IllegalArgumentException("Enter a valid phone number");
        }
    }
}
