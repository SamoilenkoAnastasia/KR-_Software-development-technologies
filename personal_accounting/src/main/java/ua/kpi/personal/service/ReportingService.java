package ua.kpi.personal.service;

import ua.kpi.personal.model.Account;
import ua.kpi.personal.model.Category;
import ua.kpi.personal.model.Goal;
import ua.kpi.personal.model.Transaction;
import ua.kpi.personal.repo.AccountDao;
import ua.kpi.personal.repo.CategoryCache;
import ua.kpi.personal.repo.GoalDao;
import ua.kpi.personal.repo.TransactionDao;
import ua.kpi.personal.model.analytics.MonthlyBalanceRow;
import ua.kpi.personal.model.analytics.ReportParams;
import ua.kpi.personal.model.analytics.CategoryReportRow;

import java.time.LocalDate;
import java.util.Collections; // Додано для Collections.emptyList()
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class ReportingService {

    private final AccountDao accountDao;
    private final GoalDao goalDao;
    private final TransactionDao transactionDao;

    private static final String BASE_CURRENCY = "UAH";
    private static final double USD_RATE = 38.0;
    private static final double EUR_RATE = 41.5;

    public ReportingService(AccountDao accountDao, GoalDao goalDao, TransactionDao transactionDao) {
        this.accountDao = accountDao;
        this.goalDao = goalDao;
        this.transactionDao = transactionDao;
    }

    private static class MonthlyDynamicsAggregator {
        final String monthYear;
        double totalIncome = 0.0;
        double totalExpense = 0.0;

        public MonthlyDynamicsAggregator(String monthYear) {
            this.monthYear = monthYear;
        }

        public void addIncome(double amount) { this.totalIncome += amount; }
        public void addExpense(double amount) { this.totalExpense += amount; }
    }

    private double convertToBase(double amount, String currency) {
        // ... (логіка конвертації) ...
        if (currency == null || currency.trim().isEmpty()) {
            return 0.0;
        }

        if (BASE_CURRENCY.equalsIgnoreCase(currency)) {
            return amount;
        }

        switch (currency.toUpperCase()) {
            case "USD": return amount * USD_RATE;
            case "EUR": return amount * EUR_RATE;
            default: return 0.0;
        }
    }

    // ВИПРАВЛЕНО: Рядок 70 - виправлено конструктор ReportParams
    public List<Transaction> findTransactionsByDateRange(Long budgetId, LocalDate startDate, LocalDate endDate) {
        if (budgetId == null) return List.of();
        
        // Повний конструктор ReportParams вимагає:
        // 1. startDate
        // 2. endDate
        // 3. List<Category> (null)
        // 4. List<Account> (null)
        // 5. String (null)
        
        ReportParams params = new ReportParams(
            startDate, 
            endDate, 
            Collections.emptyList(), // Або null, якщо дозволено
            Collections.emptyList(), // Або null, якщо дозволено
            null
        );
        return transactionDao.findTransactionsByDateRange(params, budgetId); 
    }
    
    // ... (інші методи залишаються без змін) ...
    // ... (getTotalNetWorth, getMonthlySummary, getExpensesByCategory, getMonthlyDynamics, getCategorySummary) ...

    public double getTotalNetWorth(Long budgetId) {
        if (budgetId == null) return 0.0;

        List<Account> accounts = accountDao.findByBudgetId(budgetId); // Цей метод потрібен у AccountDao
        List<Goal> goals = goalDao.findByBudgetId(budgetId); // Це виправляє поточну помилку

        double total = 0.0;

        for (Account acc : accounts) {
            total += convertToBase(acc.getBalance(), acc.getCurrency());
        }

        for (Goal goal : goals) {
            total += convertToBase(goal.getCurrentAmount(), goal.getCurrency());
        }

        return total;
    }

    
    public Map<String, Double> getMonthlySummary(Long budgetId) {
        if (budgetId == null) return Map.of("Income", 0.0, "Expense", 0.0);

        List<Transaction> transactions = transactionDao.findByBudgetId(budgetId); // Цей метод потрібен у TransactionDao

        double totalIncome = transactions.stream()
            .filter(t -> "INCOME".equalsIgnoreCase(t.getType()))
            .mapToDouble(t -> convertToBase(t.getAmount(), t.getCurrency()))
            .sum();

        double totalExpense = transactions.stream()
            .filter(t -> "EXPENSE".equalsIgnoreCase(t.getType()))
            .mapToDouble(t -> convertToBase(t.getAmount(), t.getCurrency()))
            .sum();

        return Map.of("Income", totalIncome, "Expense", totalExpense);
    }

    public Map<String, Double> getExpensesByCategory(Long budgetId) {
        if (budgetId == null) return Map.of();

        List<Transaction> transactions = transactionDao.findByBudgetId(budgetId); 
        return transactions.stream()
            .filter(t -> "EXPENSE".equalsIgnoreCase(t.getType()) && t.getCategory() != null)
            .collect(Collectors.groupingBy(
                t -> t.getCategory().getName(),
                Collectors.summingDouble(t -> convertToBase(t.getAmount(), t.getCurrency()))
            ));
    }

    // ... (методи getMonthlyDynamics та getCategorySummary) ...

    public List<MonthlyBalanceRow> getMonthlyDynamics(ReportParams params, Long budgetId) {
        
        List<Object[]> rawData = transactionDao.aggregateMonthlySummary(params, budgetId); 

        Map<String, MonthlyDynamicsAggregator> summaryMap = new TreeMap<>();

        for (Object[] row : rawData) {
            String monthYear = (String) row[0];
            String type = (String) row[1];
            double amount = (Double) row[2];
            String currency = (String) row[3];

            double convertedAmount = convertToBase(amount, currency);

            MonthlyDynamicsAggregator aggregator = summaryMap.computeIfAbsent(monthYear, MonthlyDynamicsAggregator::new);

            if ("INCOME".equalsIgnoreCase(type)) {
                aggregator.addIncome(convertedAmount);
            } else if ("EXPENSE".equalsIgnoreCase(type)) {
                aggregator.addExpense(convertedAmount);
            }
        }

        return summaryMap.values().stream()
            .map(agg -> new MonthlyBalanceRow(agg.monthYear, agg.totalIncome, agg.totalExpense))
            .collect(Collectors.toList());
    }

    
    public List<CategoryReportRow> getCategorySummary(ReportParams params, Long budgetId) {

        List<Object[]> rawData = transactionDao.aggregateByCategorySummary(params, budgetId); 

        Map<Long, Double> categoryTotals = new TreeMap<>();

        for (Object[] row : rawData) {
            Long categoryId = (Long) row[0];    // category_id
            String type = (String) row[1];      // type (INCOME/EXPENSE)
            double amount = (Double) row[2];    // total_amount
            String currency = (String) row[3];  // currency

            double convertedAmount = convertToBase(amount, currency);

            if ("EXPENSE".equalsIgnoreCase(type)) {
                convertedAmount = -convertedAmount;  
            }

            categoryTotals.merge(categoryId, convertedAmount, Double::sum);
        }

        return categoryTotals.entrySet().stream()
            .map(entry -> {
                Long categoryId = entry.getKey();
                double totalAmount = entry.getValue();

                Category category = CategoryCache.getById(categoryId);
                String categoryName = category != null ? category.getName() : "Без категорії (ID: " + categoryId + ")";

                // Створюємо DTO
                return new CategoryReportRow(categoryName, totalAmount);
            })
            .filter(row -> Math.abs(row.totalAmount()) > 0.001) // Прибираємо нульові підсумки
            .collect(Collectors.toList());
    }
}
