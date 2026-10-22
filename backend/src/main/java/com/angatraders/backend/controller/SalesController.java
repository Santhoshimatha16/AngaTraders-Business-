
package com.angatraders.backend.controller;

import com.angatraders.backend.entity.Sales;
import com.angatraders.backend.service.SalesService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/sales")
@CrossOrigin(origins = "http://localhost:5173")
public class SalesController {

    private final SalesService salesService;

    public SalesController(SalesService salesService) {
        this.salesService = salesService;
    }

    @GetMapping
    public List<Sales> getSales(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate,

            @RequestParam(required = false) Long customerId,

            @RequestParam(required = false) String quality) {

        if (startDate != null && endDate != null) {
            return salesService.getSalesBetweenDates(startDate, endDate);
        }

        if (date != null) {
            return salesService.getSalesByDate(date);
        }

        if (customerId != null) {
            return salesService.getSalesByCustomer(customerId);
        }

        if (quality != null && !quality.isBlank()) {
            return salesService.getSalesByQuality(quality);
        }

        return salesService.getAllSales();
    }

    @GetMapping("/{id}")
    public Sales getSaleById(@PathVariable Long id) {
        return salesService.getSaleById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Sales createSale(
            @RequestParam Long customerId,
            @RequestBody Sales sale) {
        return salesService.createSale(sale, customerId);
    }

    @PutMapping("/{id}")
    public Sales updateSale(
            @PathVariable Long id,
            @RequestParam Long customerId,
            @RequestBody Sales sale) {
        return salesService.updateSale(id, sale, customerId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSale(@PathVariable Long id) {
        salesService.deleteSale(id);
    }
}
