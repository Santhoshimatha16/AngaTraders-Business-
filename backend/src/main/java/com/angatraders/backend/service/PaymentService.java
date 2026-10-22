package com.angatraders.backend.service;

import com.angatraders.backend.dto.PaymentAllocationRequest;
import com.angatraders.backend.dto.PaymentRequest;
import com.angatraders.backend.entity.Company;
import com.angatraders.backend.entity.Customer;
import com.angatraders.backend.entity.Payment;
import com.angatraders.backend.entity.PaymentAllocation;
import com.angatraders.backend.entity.Purchase;
import com.angatraders.backend.entity.Sales;
import com.angatraders.backend.repository.CompanyRepository;
import com.angatraders.backend.repository.CustomerRepository;
import com.angatraders.backend.repository.PaymentAllocationRepository;
import com.angatraders.backend.repository.PaymentRepository;
import com.angatraders.backend.repository.PurchaseRepository;
import com.angatraders.backend.repository.SalesRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PaymentService {

    private static final String SUPPLIER_PAYMENT = "SUPPLIER_PAYMENT";
    private static final String CUSTOMER_RECEIPT = "CUSTOMER_RECEIPT";
    private static final BigDecimal ZERO = new BigDecimal("0.00");

    private final PaymentRepository paymentRepository;
    private final CompanyRepository companyRepository;
    private final CustomerRepository customerRepository;
    private final PurchaseRepository purchaseRepository;
    private final SalesRepository salesRepository;
    private final PaymentAllocationRepository allocationRepository;

    public PaymentService(
            PaymentRepository paymentRepository,
            CompanyRepository companyRepository,
            CustomerRepository customerRepository,
            PurchaseRepository purchaseRepository,
            SalesRepository salesRepository,
            PaymentAllocationRepository allocationRepository) {

        this.paymentRepository = paymentRepository;
        this.companyRepository = companyRepository;
        this.customerRepository = customerRepository;
        this.purchaseRepository = purchaseRepository;
        this.salesRepository = salesRepository;
        this.allocationRepository = allocationRepository;
    }

    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    public Payment getPaymentById(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Payment not found with ID: " + id));
    }

    public List<Payment> getPaymentsByDate(LocalDate date) {
        return paymentRepository.findByPaymentDate(date);
    }

    public List<Payment> getPaymentsBetweenDates(
            LocalDate startDate, LocalDate endDate) {

        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException(
                    "Start date and end date are required");
        }

        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException(
                    "Start date must be before or equal to end date");
        }

        return paymentRepository.findByPaymentDateBetween(
                startDate, endDate);
    }

    public List<Payment> getPaymentsByType(String type) {
        return paymentRepository.findByPaymentTypeIgnoreCase(type);
    }

    public List<Payment> getPaymentsByCompany(Long companyId) {
        return paymentRepository.findByCompanyId(companyId);
    }

    public List<Payment> getPaymentsByCustomer(Long customerId) {
        return paymentRepository.findByCustomerId(customerId);
    }

    @Transactional
    public Payment createPayment(PaymentRequest request) {

        validateRequest(request);

        Payment payment = new Payment();
        applyPaymentDetails(payment, request);

        LocalDateTime now = LocalDateTime.now();
        payment.setCreatedAt(now);
        payment.setUpdatedAt(now);

        payment = paymentRepository.saveAndFlush(payment);

        applyAllocations(payment, request.getAllocations());

        return payment;
    }

    @Transactional
    public Payment updatePayment(Long id, PaymentRequest request) {

        validateRequest(request);

        Payment payment = getPaymentById(id);

        // Reverse only allocations belonging to this payment.
        // A failure rolls back the complete transaction.
        reverseAllocations(payment);

        applyPaymentDetails(payment, request);
        payment.setUpdatedAt(LocalDateTime.now());

        paymentRepository.saveAndFlush(payment);

        applyAllocations(payment, request.getAllocations());

        return payment;
    }

    @Transactional
    public void deletePayment(Long id) {

        Payment payment = getPaymentById(id);

        reverseAllocations(payment);

        paymentRepository.delete(payment);
        paymentRepository.flush();
    }

    private void validateRequest(PaymentRequest request) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Payment details are required");
        }

        String type = normalizeType(request.getPaymentType());

        if (!SUPPLIER_PAYMENT.equals(type)
                && !CUSTOMER_RECEIPT.equals(type)) {
            throw new IllegalArgumentException(
                    "Payment type must be SUPPLIER_PAYMENT "
                            + "or CUSTOMER_RECEIPT");
        }

        if (request.getPaymentDate() == null) {
            throw new IllegalArgumentException(
                    "Payment date is required");
        }

        requirePositiveMoney(request.getAmount(), "Payment amount");

        List<PaymentAllocationRequest> allocations =
                request.getAllocations();

        if (allocations == null || allocations.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one payment allocation is required");
        }

        BigDecimal allocationTotal = ZERO;

        for (PaymentAllocationRequest allocation : allocations) {

            if (allocation == null) {
                throw new IllegalArgumentException(
                        "Allocation details cannot be null");
            }

            boolean hasPurchase = allocation.getPurchaseId() != null;
            boolean hasSale = allocation.getSaleId() != null;

            if (hasPurchase == hasSale) {
                throw new IllegalArgumentException(
                        "Each allocation must target exactly one "
                                + "purchase or sale");
            }

            if (SUPPLIER_PAYMENT.equals(type) && !hasPurchase) {
                throw new IllegalArgumentException(
                        "Supplier payments must be allocated to purchases");
            }

            if (CUSTOMER_RECEIPT.equals(type) && !hasSale) {
                throw new IllegalArgumentException(
                        "Customer receipts must be allocated to sales");
            }

            requirePositiveMoney(
                    allocation.getAmount(), "Allocation amount");

            allocationTotal = allocationTotal.add(
                    allocation.getAmount().setScale(
                            2, RoundingMode.UNNECESSARY));
        }

        BigDecimal paymentAmount = request.getAmount().setScale(
                2, RoundingMode.UNNECESSARY);

        if (allocationTotal.compareTo(paymentAmount) != 0) {
            throw new IllegalArgumentException(
                    "Allocation amounts must equal the payment amount");
        }

        if (SUPPLIER_PAYMENT.equals(type)) {

            if (request.getCompanyId() == null) {
                throw new IllegalArgumentException(
                        "Company ID is required for supplier payments");
            }

            if (request.getCustomerId() != null) {
                throw new IllegalArgumentException(
                        "Supplier payments cannot specify a customer");
            }

        } else {

            if (request.getCustomerId() == null) {
                throw new IllegalArgumentException(
                        "Customer ID is required for customer receipts");
            }

            if (request.getCompanyId() != null) {
                throw new IllegalArgumentException(
                        "Customer receipts cannot specify a company");
            }
        }
    }

    private void applyPaymentDetails(
            Payment payment, PaymentRequest request) {

        String type = normalizeType(request.getPaymentType());

        payment.setPaymentType(type);
        payment.setPaymentDate(request.getPaymentDate());
        payment.setAmount(request.getAmount().setScale(
                2, RoundingMode.UNNECESSARY));
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setReferenceNumber(request.getReferenceNumber());
        payment.setNotes(request.getNotes());

        if (SUPPLIER_PAYMENT.equals(type)) {

            Company company = companyRepository.findById(
                    request.getCompanyId())
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Company not found with ID: "
                                            + request.getCompanyId()));

            payment.setCompany(company);
            payment.setCustomer(null);

        } else {

            Customer customer = customerRepository.findById(
                    request.getCustomerId())
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Customer not found with ID: "
                                            + request.getCustomerId()));

            payment.setCustomer(customer);
            payment.setCompany(null);
        }
    }

    private void applyAllocations(
            Payment payment,
            List<PaymentAllocationRequest> requests) {

        for (PaymentAllocationRequest request : requests) {

            BigDecimal amount = request.getAmount().setScale(
                    2, RoundingMode.UNNECESSARY);

            PaymentAllocation allocation = new PaymentAllocation();
            allocation.setPayment(payment);

            if (SUPPLIER_PAYMENT.equals(payment.getPaymentType())) {

                // Lock the purchase row before checking its balance.
                Purchase purchase = purchaseRepository.findByIdForUpdate(
                        request.getPurchaseId())
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Purchase not found with ID: "
                                                + request.getPurchaseId()));

                if (!purchase.getCompany().getId().equals(
                        payment.getCompany().getId())) {
                    throw new IllegalArgumentException(
                            "Purchase does not belong to the selected company");
                }

                BigDecimal pending = money(purchase.getAmountPending());

                if (pending.compareTo(ZERO) < 0) {
                    throw new IllegalStateException(
                            "Purchase has an invalid negative pending balance");
                }

                if (amount.compareTo(pending) > 0) {
                    throw new IllegalArgumentException(
                            "Allocation exceeds outstanding purchase balance: "
                                    + purchase.getId());
                }

                BigDecimal newPaid = money(purchase.getAmountPaid())
                        .add(amount);
                BigDecimal newPending = pending.subtract(amount);

                validateBalances(
                        purchase.getTotalAmount(), newPaid, newPending,
                        "purchase " + purchase.getId());

                purchase.setAmountPaid(newPaid);
                purchase.setAmountPending(newPending);

                purchaseRepository.save(purchase);

                allocation.setPurchase(purchase);
                allocation.setSale(null);

            } else {

                // Lock the sale row before checking its balance.
                Sales sale = salesRepository.findByIdForUpdate(
                        request.getSaleId())
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Sale not found with ID: "
                                                + request.getSaleId()));

                if (!sale.getCustomer().getId().equals(
                        payment.getCustomer().getId())) {
                    throw new IllegalArgumentException(
                            "Sale does not belong to the selected customer");
                }

                BigDecimal pending = money(sale.getAmountPending());

                if (pending.compareTo(ZERO) < 0) {
                    throw new IllegalStateException(
                            "Sale has an invalid negative pending balance");
                }

                if (amount.compareTo(pending) > 0) {
                    throw new IllegalArgumentException(
                            "Allocation exceeds outstanding sale balance: "
                                    + sale.getId());
                }

                BigDecimal newPaid = money(sale.getAmountPaid())
                        .add(amount);
                BigDecimal newPending = pending.subtract(amount);

                validateBalances(
                        sale.getTotalAmount(), newPaid, newPending,
                        "sale " + sale.getId());

                sale.setAmountPaid(newPaid);
                sale.setAmountPending(newPending);

                salesRepository.save(sale);

                allocation.setSale(sale);
                allocation.setPurchase(null);
            }

            LocalDateTime now = LocalDateTime.now();
            allocation.setCreatedAt(now);
            allocation.setUpdatedAt(now);

            allocationRepository.save(allocation);
        }

        allocationRepository.flush();
    }

    private void reverseAllocations(Payment payment) {

        List<PaymentAllocation> allocations =
                allocationRepository.findByPaymentId(payment.getId());

        for (PaymentAllocation allocation : allocations) {

            BigDecimal amount = money(allocation.getAllocatedAmount());

            if (amount.compareTo(ZERO) <= 0) {
                throw new IllegalStateException(
                        "Cannot reverse an invalid allocation amount");
            }

            if (allocation.getPurchase() != null) {

                // Lock the purchase row before reversing the allocation.
                Purchase purchase = purchaseRepository.findByIdForUpdate(
                        allocation.getPurchase().getId())
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Allocated purchase no longer exists"));

                BigDecimal paid = money(purchase.getAmountPaid());

                if (paid.compareTo(amount) < 0) {
                    throw new IllegalStateException(
                            "Cannot reverse allocation: purchase paid "
                                    + "balance is inconsistent");
                }

                BigDecimal newPaid = paid.subtract(amount);
                BigDecimal newPending = money(purchase.getTotalAmount())
                        .subtract(newPaid);

                validateBalances(
                        purchase.getTotalAmount(), newPaid, newPending,
                        "purchase " + purchase.getId());

                purchase.setAmountPaid(newPaid);
                purchase.setAmountPending(newPending);

                purchaseRepository.save(purchase);
            }

            if (allocation.getSale() != null) {

                // Lock the sale row before reversing the allocation.
                Sales sale = salesRepository.findByIdForUpdate(
                        allocation.getSale().getId())
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Allocated sale no longer exists"));

                BigDecimal paid = money(sale.getAmountPaid());

                if (paid.compareTo(amount) < 0) {
                    throw new IllegalStateException(
                            "Cannot reverse allocation: sale paid "
                                    + "balance is inconsistent");
                }

                BigDecimal newPaid = paid.subtract(amount);
                BigDecimal newPending = money(sale.getTotalAmount())
                        .subtract(newPaid);

                validateBalances(
                        sale.getTotalAmount(), newPaid, newPending,
                        "sale " + sale.getId());

                sale.setAmountPaid(newPaid);
                sale.setAmountPending(newPending);

                salesRepository.save(sale);
            }
        }

        allocationRepository.deleteAll(allocations);
        allocationRepository.flush();
    }

    private void validateBalances(
            BigDecimal total,
            BigDecimal paid,
            BigDecimal pending,
            String record) {

        BigDecimal normalizedTotal = money(total);
        BigDecimal normalizedPaid = money(paid);
        BigDecimal normalizedPending = money(pending);

        if (normalizedTotal.compareTo(ZERO) < 0
                || normalizedPaid.compareTo(ZERO) < 0
                || normalizedPending.compareTo(ZERO) < 0) {
            throw new IllegalStateException(
                    "Invalid financial balance for " + record);
        }

        if (normalizedPaid.add(normalizedPending)
                .compareTo(normalizedTotal) != 0) {
            throw new IllegalStateException(
                    "Paid and pending balances do not equal the total for "
                            + record);
        }
    }

    private void requirePositiveMoney(
            BigDecimal amount, String fieldName) {

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    fieldName + " must be greater than zero");
        }

        if (amount.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException(
                    fieldName + " cannot have more than two decimal places");
        }
    }

    private BigDecimal money(BigDecimal amount) {

        if (amount == null) {
            return ZERO;
        }

        return amount.setScale(2, RoundingMode.UNNECESSARY);
    }

    private String normalizeType(String type) {
        return type == null ? "" : type.trim().toUpperCase();
    }
}