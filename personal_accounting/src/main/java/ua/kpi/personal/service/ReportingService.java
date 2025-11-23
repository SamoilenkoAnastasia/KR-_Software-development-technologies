package ua.kpi.personal.service;

import ua.kpi.personal.model.Account;
import ua.kpi.personal.model.Category;
import ua.kpi.personal.model.Goal;
import ua.kpi.personal.model.Transaction;
import ua.kpi.personal.model.User;
import ua.kpi.personal.repo.AccountDao;
import ua.kpi.personal.repo.CategoryCache; // Додано імпорт
import ua.kpi.personal.repo.GoalDao;
import ua.kpi.personal.repo.TransactionDao;
import ua.kpi.personal.model.analytics.MonthlyBalanceRow;
import ua.kpi.personal.model.analytics.ReportParams;
import ua.kpi.personal.model.analytics.CategoryReportRow; // Додано імпорт

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

    /**
     * Конвертує суму вказаної валюти у базову валюту (UAH).
     * @param amount Сума
     * @param currency Код валюти (наприклад, "USD", "EUR")
     * @return Конвертована сума в UAH, або 0.0, якщо валюта невідома або null.
     */
    private double convertToBase(double amount, String currency) {
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
    
    public double getTotalNetWorth(User user) {
        if (user == null) return 0.0;
        
        List<Account> accounts = accountDao.findByUserId(user.getId());
        List<Goal> goals = goalDao.findByUserId(user.getId());
        double total = 0.0;

        for (Account acc : accounts) {
            total += convertToBase(acc.getBalance(), acc.getCurrency());
        }

        for (Goal goal : goals) {
            total += convertToBase(goal.getCurrentAmount(), goal.getCurrency());
        }

        return total;
    }

    public Map<String, Double> getMonthlySummary(User user) {
        if (user == null) return Map.of("Income", 0.0, "Expense", 0.0);
        
        // Примітка: Цей метод повертає дані лише для *усіх* транзакцій користувача,
        // не використовуючи ReportParams, якщо це не передбачено дизайном.
        List<Transaction> transactions = transactionDao.findByUserId(user.getId());
        
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
    
    public Map<String, Double> getExpensesByCategory(User user) {
        // Залишено, як було, але зауважте: цей метод отримує ВСІ транзакції, не фільтруючи за датами.
        // Для звітів краще використовувати метод getCategorySummary, який використовує ReportParams.
        if (user == null) return Map.of();
        
        List<Transaction> transactions = transactionDao.findByUserId(user.getId());

        return transactions.stream()
            .filter(t -> "EXPENSE".equalsIgnoreCase(t.getType()) && t.getCategory() != null)
            .collect(Collectors.groupingBy(
                t -> t.getCategory().getName(),
                Collectors.summingDouble(t -> convertToBase(t.getAmount(), t.getCurrency()))
            ));
    }

    /**
     * Розраховує та повертає зведену динаміку доходів і витрат за місяцями
     * у базовій валюті (UAH) для обраного періоду.
     */
    public List<MonthlyBalanceRow> getMonthlyDynamics(ReportParams params, User user) {
        List<Object[]> rawData = transactionDao.aggregateMonthlySummary(params, user.getId());
        
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

    // =======================================================
    // ? getCategorySummary (Зведена статистика за категоріями)
    // =======================================================
    /**
     * Розраховує та повертає зведені суми транзакцій за категоріями у базовій валюті (UAH) 
     * для обраного періоду.
     */
    public List<CategoryReportRow> getCategorySummary(ReportParams params, User user) {
        
        List<Object[]> rawData = transactionDao.aggregateByCategorySummary(params, user.getId());

        // Використовуємо Map<Long, Double> для зведення загальних сум за categoryId
        Map<Long, Double> categoryTotals = new TreeMap<>(); 
        
        for (Object[] row : rawData) {
            Long categoryId = (Long) row[0];     // category_id
            String type = (String) row[1];       // type (INCOME/EXPENSE)
            double amount = (Double) row[2];     // total_amount
            String currency = (String) row[3];   // currency
            
            double convertedAmount = convertToBase(amount, currency);
            
            // Витрати віднімаємо, доходи додаємо (чи просто додаємо, якщо агрегація потрібна лише для витрат)
            // Припускаємо, що звіт може містити доходи та витрати, зведені в одну суму для категорії
            // (наприклад, "Інвестиції" можуть мати і дохід, і витрату).
            if ("EXPENSE".equalsIgnoreCase(type)) {
                 convertedAmount = -convertedAmount; // Віднімаємо витрати
            }

            categoryTotals.merge(categoryId, convertedAmount, Double::sum);
        }
        
        // Конвертація у фінальний DTO (CategoryReportRow)
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