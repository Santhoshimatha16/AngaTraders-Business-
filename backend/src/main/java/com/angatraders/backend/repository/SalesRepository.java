package com.angatraders.backend.repository;

import com.angatraders.backend.entity.Sales;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SalesRepository extends JpaRepository<Sales, Long> {

    List<Sales> findBySaleDate(LocalDate saleDate);

    List<Sales> findByCustomerId(Long customerId);

    List<Sales> findBySaleDateBetween(
            LocalDate startDate,
            LocalDate endDate
    );

    List<Sales> findByQualityIgnoreCase(String quality);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Sales s WHERE s.id = :id")
    Optional<Sales> findByIdForUpdate(@Param("id") Long id);
}
