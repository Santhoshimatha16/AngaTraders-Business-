
package com.angatraders.backend.controller;

import com.angatraders.backend.entity.Purchase;
import com.angatraders.backend.service.PurchaseService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/purchases")
@CrossOrigin(origins = "http://localhost:5173")
public class PurchaseController {

    private final PurchaseService purchaseService;

    public PurchaseController(PurchaseService purchaseService) {
        this.purchaseService = purchaseService;
    }

    @GetMapping
    public List<Purchase> getPurchases(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate,

            @RequestParam(required = false) Long companyId) {

        if (startDate != null && endDate != null) {
            return purchaseService.getPurchasesBetweenDates(startDate, endDate);
        }

        if (date != null) {
            return purchaseService.getPurchasesByDate(date);
        }

        if (companyId != null) {
            return purchaseService.getPurchasesByCompany(companyId);
        }

        return purchaseService.getAllPurchases();
    }

    @GetMapping("/{id}")
    public Purchase getPurchaseById(@PathVariable Long id) {
        return purchaseService.getPurchaseById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Purchase createPurchase(
            @RequestParam Long companyId,
            @RequestBody Purchase purchase) {
        return purchaseService.createPurchase(purchase, companyId);
    }

    @PutMapping("/{id}")
    public Purchase updatePurchase(
            @PathVariable Long id,
            @RequestParam Long companyId,
            @RequestBody Purchase purchase) {
        return purchaseService.updatePurchase(id, purchase, companyId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePurchase(@PathVariable Long id) {
        purchaseService.deletePurchase(id);
    }
}
