package ua.kpi.personal.model.analytics;

public record MonthlyBalanceRow(
    String monthYear, 
    double totalIncome,
    double totalExpense
) {}