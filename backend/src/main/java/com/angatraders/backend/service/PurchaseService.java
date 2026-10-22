package com.angatraders.backend.service;

import com.angatraders.backend.entity.Company;
import com.angatraders.backend.entity.Purchase;
import com.angatraders.backend.repository.CompanyRepository;
import com.angatraders.backend.repository.PurchaseRepository;
import com.angatraders.backend.repository.PaymentAllocationRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PurchaseService {

    private final PurchaseRepository purchaseRepository;
    private final CompanyRepository companyRepository;
    private final PaymentAllocationRepository allocationRepository;

    public PurchaseService(
            PurchaseRepository purchaseRepository,
            CompanyRepository companyRepository,
            PaymentAllocationRepository allocationRepository) {
        this.purchaseRepository = purchaseRepository;
        this.companyRepository = companyRepository;
        this.allocationRepository = allocationRepository;
    }

    public List<Purchase> getAllPurchases() {
        return purchaseRepository.findAll();
    }

    public Purchase getPurchaseById(Long id) {
        return purchaseRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Purchase not found with ID: " + id));
    }

    public List<Purchase> getPurchasesByDate(LocalDate date) {
        return purchaseRepository.findByPurchaseDate(date);
    }

    public List<Purchase> getPurchasesByCompany(Long companyId) {
        return purchaseRepository.findByCompanyId(companyId);
    }

    public List<Purchase> getPurchasesBetweenDates(
            LocalDate startDate, LocalDate endDate) {

        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException(
                    "Start date and end date are required");
        }

        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException(
                    "Start date must be before or equal to end date");
        }

        return purchaseRepository.findByPurchaseDateBetween(
                startDate, endDate);
    }

    @Transactional
    public Purchase createPurchase(Purchase purchase, Long companyId) {

        if (purchase == null) {
            throw new IllegalArgumentException(
                    "Purchase details are required");
        }

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Company not found with ID: " + companyId));

        // All payments must be recorded through the Payments API.
        if (purchase.getAmountPaid() != null
                && purchase.getAmountPaid().compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalArgumentException(
                    "Record purchase payments through the Payments API");
        }

        purchase.setAmountPaid(BigDecimal.ZERO);

        validatePurchase(purchase);

        BigDecimal totalAmount = calculateTotal(
                purchase.getNumberOfBoxes(),
                purchase.getPurchaseRate());

        purchase.setCompany(company);
        purchase.setTotalAmount(totalAmount);
        purchase.setAmountPending(totalAmount);

        LocalDateTime now = LocalDateTime.now();
        purchase.setCreatedAt(now);
        purchase.setUpdatedAt(now);

        return purchaseRepository.save(purchase);
    }

    @Transactional
    public Purchase updatePurchase(
            Long id, Purchase updatedPurchase, Long companyId) {

        if (updatedPurchase == null) {
            throw new IllegalArgumentException(
                    "Updated purchase details are required");
        }

        Purchase existing = getPurchaseById(id);

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Company not found with ID: " + companyId));

        // Never accept a replacement paid amount from the request.
        BigDecimal existingPaid = money(existing.getAmountPaid());

        updatedPurchase.setAmountPaid(existingPaid);
        validatePurchase(updatedPurchase);

        BigDecimal totalAmount = calculateTotal(
                updatedPurchase.getNumberOfBoxes(),
                updatedPurchase.getPurchaseRate());

        if (totalAmount.compareTo(existingPaid) < 0) {
            throw new IllegalArgumentException(
                    "Total purchase amount cannot be less than the amount already paid");
        }

        existing.setCompany(company);
        existing.setPurchaseDate(updatedPurchase.getPurchaseDate());
        existing.setQuality(updatedPurchase.getQuality());
        existing.setNumberOfBoxes(updatedPurchase.getNumberOfBoxes());
        existing.setPurchaseRate(updatedPurchase.getPurchaseRate());
        existing.setTotalAmount(totalAmount);

        // Preserve the paid amount; only recalculate pending.
        existing.setAmountPaid(existingPaid);
        existing.setAmountPending(
                totalAmount.subtract(existingPaid)
                        .setScale(2, RoundingMode.HALF_UP));

        existing.setNotes(updatedPurchase.getNotes());
        existing.setUpdatedAt(LocalDateTime.now());

        return purchaseRepository.save(existing);
    }

    @Transactional
    public void deletePurchase(Long id) {

        Purchase purchase = getPurchaseById(id);

        if (!allocationRepository.findByPurchaseId(id).isEmpty()) {
            throw new IllegalArgumentException(
                    "Cannot delete this purchase because payment allocations exist");
        }

        if (money(purchase.getAmountPaid()).compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalArgumentException(
                    "Cannot delete a purchase with recorded payments");
        }

        purchaseRepository.delete(purchase);
    }

    private void validatePurchase(Purchase purchase) {

        if (purchase.getPurchaseDate() == null) {
            throw new IllegalArgumentException(
                    "Purchase date is required");
        }

        if (purchase.getNumberOfBoxes() == null
                || purchase.getNumberOfBoxes() <= 0) {
            throw new IllegalArgumentException(
                    "Number of boxes must be greater than zero");
        }

        if (purchase.getPurchaseRate() == null
                || purchase.getPurchaseRate()
                        .compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "Purchase rate cannot be negative");
        }

        if (purchase.getAmountPaid() == null
                || purchase.getAmountPaid()
                        .compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "Amount paid cannot be negative");
        }

        BigDecimal totalAmount = calculateTotal(
                purchase.getNumberOfBoxes(),
                purchase.getPurchaseRate());

        if (purchase.getAmountPaid().compareTo(totalAmount) > 0) {
            throw new IllegalArgumentException(
                    "Amount paid cannot exceed the total purchase amount");
        }
    }

    private BigDecimal calculateTotal(int boxes, BigDecimal rate) {
        return BigDecimal.valueOf(boxes)
                .multiply(rate)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal money(BigDecimal amount) {
        return amount == null
                ? BigDecimal.ZERO.setScale(2)
                : amount.setScale(2, RoundingMode.HALF_UP);
    }
}
