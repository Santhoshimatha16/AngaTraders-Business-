package com.angatraders.backend.controller;

import com.angatraders.backend.dto.PaymentRequest;
import com.angatraders.backend.entity.Payment;
import com.angatraders.backend.service.PaymentService;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "http://localhost:5173")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping
    public List<Payment> getAllPayments(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate,

            @RequestParam(required = false) String type,
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) Long customerId) {

        if (date != null) {
            return paymentService.getPaymentsByDate(date);
        }

        if (startDate != null || endDate != null) {
            if (startDate == null || endDate == null) {
                throw new IllegalArgumentException(
                        "Both startDate and endDate are required");
            }

            return paymentService.getPaymentsBetweenDates(
                    startDate, endDate);
        }

        if (type != null && !type.isBlank()) {
            return paymentService.getPaymentsByType(type);
        }

        if (companyId != null) {
            return paymentService.getPaymentsByCompany(companyId);
        }

        if (customerId != null) {
            return paymentService.getPaymentsByCustomer(customerId);
        }

        return paymentService.getAllPayments();
    }

    @GetMapping("/{id}")
    public Payment getPaymentById(@PathVariable Long id) {
        return paymentService.getPaymentById(id);
    }

    @PostMapping
    public Payment createPayment(
            @RequestBody PaymentRequest request) {
        return paymentService.createPayment(request);
    }

    @PutMapping("/{id}")
    public Payment updatePayment(
            @PathVariable Long id,
            @RequestBody PaymentRequest request) {
        return paymentService.updatePayment(id, request);
    }

    @DeleteMapping("/{id}")
    public String deletePayment(@PathVariable Long id) {
        paymentService.deletePayment(id);
        return "Payment deleted successfully";
    }
}