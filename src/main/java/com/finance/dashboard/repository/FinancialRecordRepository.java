package com.finance.dashboard.repository;

import com.finance.dashboard.entity.FinancialRecord;
import com.finance.dashboard.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FinancialRecordRepository extends JpaRepository<FinancialRecord, Long> {


    Optional<FinancialRecord> findByIdAndDeletedFalse(Long id);


    @Query("""
            SELECT r FROM FinancialRecord r
            WHERE r.deleted = false
              AND (:type     IS NULL OR r.type     = :type)
              AND (:category IS NULL OR LOWER(r.category) LIKE LOWER(CONCAT('%', :category, '%')))
              AND (:from     IS NULL OR r.transactionDate >= :from)
              AND (:to       IS NULL OR r.transactionDate <= :to)
            """)
    Page<FinancialRecord> findAllWithFilters(
            @Param("type")     TransactionType type,
            @Param("category") String category,
            @Param("from")     LocalDate from,
            @Param("to")       LocalDate to,
            Pageable pageable);



    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM FinancialRecord r WHERE r.deleted = false AND r.type = :type")
    BigDecimal sumByType(@Param("type") TransactionType type);

    @Query("""
            SELECT r.category, COALESCE(SUM(r.amount), 0)
            FROM FinancialRecord r
            WHERE r.deleted = false AND r.type = :type
            GROUP BY r.category
            ORDER BY 2 DESC
            """)
    List<Object[]> sumByCategory(@Param("type") TransactionType type);

    @Query("""
            SELECT FUNCTION('YEAR',  r.transactionDate),
                   FUNCTION('MONTH', r.transactionDate),
                   r.type,
                   COALESCE(SUM(r.amount), 0)
            FROM FinancialRecord r
            WHERE r.deleted = false
              AND r.transactionDate >= :from
            GROUP BY FUNCTION('YEAR',  r.transactionDate),
                     FUNCTION('MONTH', r.transactionDate),
                     r.type
            ORDER BY 1 ASC, 2 ASC
            """)
    List<Object[]> monthlyTrends(@Param("from") LocalDate from);


    @Query("SELECT r FROM FinancialRecord r WHERE r.deleted = false ORDER BY r.createdAt DESC")
    List<FinancialRecord> findRecentActivity(Pageable pageable);
}