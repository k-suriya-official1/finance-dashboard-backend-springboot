package com.finance.dashboard.service.impl;

import com.finance.dashboard.dto.response.DashboardSummaryResponse;
import com.finance.dashboard.dto.response.DashboardSummaryResponse.MonthlyTrend;
import com.finance.dashboard.dto.response.FinancialRecordResponse;
import com.finance.dashboard.enums.TransactionType;
import com.finance.dashboard.repository.FinancialRecordRepository;
import com.finance.dashboard.service.DashboardService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
public class DashboardServiceImpl implements DashboardService {

    private final FinancialRecordRepository recordRepository;
    private final FinancialRecordServiceImpl recordMapper;

    public DashboardServiceImpl(FinancialRecordRepository recordRepository,
                                FinancialRecordServiceImpl recordMapper) {
        this.recordRepository = recordRepository;
        this.recordMapper = recordMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary() {

       
        BigDecimal totalIncome   = recordRepository.sumByType(TransactionType.INCOME);
        BigDecimal totalExpenses = recordRepository.sumByType(TransactionType.EXPENSE);
        BigDecimal netBalance    = totalIncome.subtract(totalExpenses);

     
        Map<String, BigDecimal> incomeByCategory   = buildCategoryMap(TransactionType.INCOME);
        Map<String, BigDecimal> expenseByCategory  = buildCategoryMap(TransactionType.EXPENSE);

        List<FinancialRecordResponse> recentActivity = recordRepository
                .findRecentActivity(PageRequest.of(0, 5))
                .stream()
                .map(recordMapper::toResponse)
                .toList();

        LocalDate sixMonthsAgo = LocalDate.now().minusMonths(6).withDayOfMonth(1);
        List<MonthlyTrend> monthlyTrends = buildMonthlyTrends(sixMonthsAgo);

        return new DashboardSummaryResponse(
                totalIncome,
                totalExpenses,
                netBalance,
                incomeByCategory,
                expenseByCategory,
                recentActivity,
                monthlyTrends
        );
    }


    private Map<String, BigDecimal> buildCategoryMap(TransactionType type) {
        Map<String, BigDecimal> map = new LinkedHashMap<>();
        recordRepository.sumByCategory(type)
                .forEach(row -> map.put((String) row[0], (BigDecimal) row[1]));
        return map;
    }

    private List<MonthlyTrend> buildMonthlyTrends(LocalDate from) {

        List<Object[]> rows = recordRepository.monthlyTrends(from);

        Map<String, MonthlyTrendBuilder> trendMap = new LinkedHashMap<>();

        for (Object[] row : rows) {
            int year  = ((Number) row[0]).intValue();
            int month = ((Number) row[1]).intValue();
            TransactionType type = TransactionType.valueOf(row[2].toString());
            BigDecimal amount = (BigDecimal) row[3];

            String key = year + "-" + String.format("%02d", month);
            trendMap.computeIfAbsent(key, k -> new MonthlyTrendBuilder(year, month));

            if (type == TransactionType.INCOME) {
                trendMap.get(key).income = amount;
            } else {
                trendMap.get(key).expense = amount;
            }
        }

        return trendMap.values().stream()
                .map(b -> new MonthlyTrend(
                        b.year,
                        b.month,
                        b.income,
                        b.expense
                ))
                .toList();
    }


    private static class MonthlyTrendBuilder {
        final int year;
        final int month;
        BigDecimal income  = BigDecimal.ZERO;
        BigDecimal expense = BigDecimal.ZERO;

        MonthlyTrendBuilder(int year, int month) {
            this.year  = year;
            this.month = month;
        }
    }
}