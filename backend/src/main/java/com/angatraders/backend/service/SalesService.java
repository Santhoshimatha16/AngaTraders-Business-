
package com.angatraders.backend.service;

import com.angatraders.backend.entity.Customer;
import com.angatraders.backend.entity.Sales;
import com.angatraders.backend.repository.CustomerRepository;
import com.angatraders.backend.repository.SalesRepository;
import com.angatraders.backend.repository.PaymentAllocationRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class SalesService {

    private final SalesRepository salesRepository;
    private final CustomerRepository customerRepository;
    private final PaymentAllocationRepository allocationRepository;

    public SalesService(
            SalesRepository salesRepository,
            CustomerRepository customerRepository,
            PaymentAllocationRepository allocationRepository) {
        this.salesRepository = salesRepository;
        this.customerRepository = customerRepository;
        this.allocationRepository = allocationRepository;
    }

    public List<Sales> getAllSales() {
        return salesRepository.findAll();
    }

    public Sales getSaleById(Long id) {
        return salesRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Sale not found with ID: " + id));
    }

    public List<Sales> getSalesByDate(LocalDate date) {
        return salesRepository.findBySaleDate(date);
    }

    public List<Sales> getSalesByCustomer(Long customerId) {
        return salesRepository.findByCustomerId(customerId);
    }

    public List<Sales> getSalesBetweenDates(
            LocalDate startDate, LocalDate endDate) {

        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException(
                    "Start date and end date are required");
        }

        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException(
                    "Start date must be before or equal to end date");
        }

        return salesRepository.findBySaleDateBetween(
                startDate, endDate);
    }

    public List<Sales> getSalesByQuality(String quality) {
        return salesRepository.findByQualityIgnoreCase(quality);
    }

    @Transactional
    public Sales createSale(Sales sale, Long customerId) {

        if (sale == null) {
            throw new IllegalArgumentException(
                    "Sale details are required");
        }

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Customer not found with ID: " + customerId));

        // Payments must be recorded through the Payments API.
        if (sale.getAmountPaid() != null
                && sale.getAmountPaid().compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalArgumentException(
                    "Record customer receipts through the Payments API");
        }

        sale.setAmountPaid(BigDecimal.ZERO);
        validateSale(sale);

        BigDecimal totalAmount = calculateTotal(
                sale.getNumberOfBoxes(), sale.getSaleRate());

        sale.setCustomer(customer);
        sale.setTotalAmount(totalAmount);
        sale.setAmountPending(totalAmount);

        LocalDateTime now = LocalDateTime.now();
        sale.setCreatedAt(now);
        sale.setUpdatedAt(now);

        return salesRepository.save(sale);
    }

    @Transactional
    public Sales updateSale(
            Long id, Sales updatedSale, Long customerId) {

        if (updatedSale == null) {
            throw new IllegalArgumentException(
                    "Updated sale details are required");
        }

        Sales existing = getSaleById(id);

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Customer not found with ID: " + customerId));

        // Preserve the paid amount already recorded in the database.
        BigDecimal existingPaid = money(existing.getAmountPaid());

        updatedSale.setAmountPaid(existingPaid);
        validateSale(updatedSale);

        BigDecimal totalAmount = calculateTotal(
                updatedSale.getNumberOfBoxes(),
                updatedSale.getSaleRate());

        if (totalAmount.compareTo(existingPaid) < 0) {
            throw new IllegalArgumentException(
                    "Total sale amount cannot be less than the amount already paid");
        }

        existing.setCustomer(customer);
        existing.setSaleDate(updatedSale.getSaleDate());
        existing.setQuality(updatedSale.getQuality());
        existing.setNumberOfBoxes(updatedSale.getNumberOfBoxes());
        existing.setSaleRate(updatedSale.getSaleRate());
        existing.setTotalAmount(totalAmount);
        existing.setAmountPaid(existingPaid);
        existing.setAmountPending(
                totalAmount.subtract(existingPaid)
                        .setScale(2, RoundingMode.HALF_UP));
        existing.setNotes(updatedSale.getNotes());
        existing.setUpdatedAt(LocalDateTime.now());

        return salesRepository.save(existing);
    }

    @Transactional
    public void deleteSale(Long id) {

        Sales sale = getSaleById(id);

        if (!allocationRepository.findBySaleId(id).isEmpty()) {
            throw new IllegalArgumentException(
                    "Cannot delete this sale because payment allocations exist");
        }

        if (money(sale.getAmountPaid()).compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalArgumentException(
                    "Cannot delete a sale with recorded payments");
        }

        salesRepository.delete(sale);
    }

    private BigDecimal calculateTotal(
            Integer numberOfBoxes, BigDecimal saleRate) {

        return saleRate.multiply(BigDecimal.valueOf(numberOfBoxes))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal money(BigDecimal amount) {
        return amount == null
                ? BigDecimal.ZERO.setScale(2)
                : amount.setScale(2, RoundingMode.HALF_UP);
    }

    private void validateSale(Sales sale) {

        if (sale.getSaleDate() == null) {
            throw new IllegalArgumentException(
                    "Sale date is required");
        }

        if (sale.getNumberOfBoxes() == null
                || sale.getNumberOfBoxes() <= 0) {
            throw new IllegalArgumentException(
                    "Number of boxes must be greater than zero");
        }

        if (sale.getSaleRate() == null
                || sale.getSaleRate().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "Sale rate cannot be negative");
        }

        if (sale.getAmountPaid() == null
                || sale.getAmountPaid().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "Amount paid cannot be negative");
        }

        BigDecimal totalAmount = calculateTotal(
                sale.getNumberOfBoxes(), sale.getSaleRate());

        if (sale.getAmountPaid().compareTo(totalAmount) > 0) {
            throw new IllegalArgumentException(
                    "Amount paid cannot exceed the total sale amount");
        }

        if (sale.getQuality() == null
                || sale.getQuality().isBlank()) {
            throw new IllegalArgumentException(
                    "Mushroom quality is required");
        }
    }
}