package com.finance.dashboard.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class DashboardSummaryResponse {

    private BigDecimal totalIncome;
    private BigDecimal totalExpenses;
    private BigDecimal netBalance;
    private Map<String, BigDecimal> incomeByCategory;

    private Map<String, BigDecimal> expenseByCategory;

    private List<FinancialRecordResponse> recentActivity;
    private List<MonthlyTrend> monthlyTrends;


    public DashboardSummaryResponse() {
    }

    public DashboardSummaryResponse(BigDecimal totalIncome, BigDecimal totalExpenses, BigDecimal netBalance,
                                    Map<String, BigDecimal> incomeByCategory,
                                    Map<String, BigDecimal> expenseByCategory,
                                    List<FinancialRecordResponse> recentActivity,
                                    List<MonthlyTrend> monthlyTrends) {
        this.totalIncome = totalIncome;
        this.totalExpenses = totalExpenses;
        this.netBalance = netBalance;
        this.incomeByCategory = incomeByCategory;
        this.expenseByCategory = expenseByCategory;
        this.recentActivity = recentActivity;
        this.monthlyTrends = monthlyTrends;
    }


    public BigDecimal getTotalIncome() {
        return totalIncome;
    }

    public void setTotalIncome(BigDecimal totalIncome) {
        this.totalIncome = totalIncome;
    }

    public BigDecimal getTotalExpenses() {
        return totalExpenses;
    }

    public void setTotalExpenses(BigDecimal totalExpenses) {
        this.totalExpenses = totalExpenses;
    }

    public BigDecimal getNetBalance() {
        return netBalance;
    }

    public void setNetBalance(BigDecimal netBalance) {
        this.netBalance = netBalance;
    }

    public Map<String, BigDecimal> getIncomeByCategory() {
        return incomeByCategory;
    }

    public void setIncomeByCategory(Map<String, BigDecimal> incomeByCategory) {
        this.incomeByCategory = incomeByCategory;
    }

    public Map<String, BigDecimal> getExpenseByCategory() {
        return expenseByCategory;
    }

    public void setExpenseByCategory(Map<String, BigDecimal> expenseByCategory) {
        this.expenseByCategory = expenseByCategory;
    }

    public List<FinancialRecordResponse> getRecentActivity() {
        return recentActivity;
    }

    public void setRecentActivity(List<FinancialRecordResponse> recentActivity) {
        this.recentActivity = recentActivity;
    }

    public List<MonthlyTrend> getMonthlyTrends() {
        return monthlyTrends;
    }

    public void setMonthlyTrends(List<MonthlyTrend> monthlyTrends) {
        this.monthlyTrends = monthlyTrends;
    }


    public static class MonthlyTrend {

        private int year;
        private int month;
        private BigDecimal income;
        private BigDecimal expense;


        public MonthlyTrend() {
        }

        public MonthlyTrend(int year, int month, BigDecimal income, BigDecimal expense) {
            this.year = year;
            this.month = month;
            this.income = income;
            this.expense = expense;
        }


        public int getYear() {
            return year;
        }

        public void setYear(int year) {
            this.year = year;
        }

        public int getMonth() {
            return month;
        }

        public void setMonth(int month) {
            this.month = month;
        }

        public BigDecimal getIncome() {
            return income;
        }

        public void setIncome(BigDecimal income) {
            this.income = income;
        }

        public BigDecimal getExpense() {
            return expense;
        }

        public void setExpense(BigDecimal expense) {
            this.expense = expense;
        }
    }
}