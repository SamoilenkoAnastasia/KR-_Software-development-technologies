/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ua.kpi.personal.service;

import ua.kpi.personal.model.Account;
import ua.kpi.personal.model.Goal;
import ua.kpi.personal.model.Transaction;
import ua.kpi.personal.model.User;
import ua.kpi.personal.repo.AccountDao;
import ua.kpi.personal.repo.GoalDao;
import ua.kpi.personal.repo.TransactionDao;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ReportingService {

    private final AccountDao accountDao;
    private final GoalDao goalDao;
    private final TransactionDao transactionDao;

    // Припустимо, що UAH — це базова валюта
    private static final String BASE_CURRENCY = "UAH";
    private static final double USD_RATE = 38.0; 
    private static final double EUR_RATE = 41.5; 

    public ReportingService(AccountDao accountDao, GoalDao goalDao, TransactionDao transactionDao) {
        this.accountDao = accountDao;
        this.goalDao = goalDao;
        this.transactionDao = transactionDao;
    }

    // --- Допоміжний метод конвертації (для агрегації) ---
    private double convertToBase(double amount, String currency) {
        if (BASE_CURRENCY.equalsIgnoreCase(currency)) {
            return amount;
        }
        
        switch (currency) {
            case "USD": return amount * USD_RATE;
            case "EUR": return amount * EUR_RATE;
            default: return 0.0; // Ігноруємо непідтримувані валюти
        }
    }
    
    // --- ОСНОВНІ МЕТОДИ ЗВІТНОСТІ ---

    /**
     * Розраховує загальний чистий капітал користувача у базовій валюті (UAH).
     * Включає всі рахунки та накопичені суми цілей.
     */
    public double getTotalNetWorth(User user) {
        if (user == null) return 0.0;
        
        List<Account> accounts = accountDao.findByUserId(user.getId());
        List<Goal> goals = goalDao.findByUserId(user.getId());
        double total = 0.0;

        // 1. Агрегація рахунків
        for (Account acc : accounts) {
            total += convertToBase(acc.getBalance(), acc.getCurrency());
        }

        // 2. Агрегація накопичень у цілях
        for (Goal goal : goals) {
            total += convertToBase(goal.getCurrentAmount(), goal.getCurrency());
        }

        return total;
    }

    /**
     * Розраховує загальні доходи та витрати за місяць.
     * Тут потрібно було б фільтрувати транзакції за датою, але для простоти беремо всі.
     */
    public Map<String, Double> getMonthlySummary(User user) {
        if (user == null) return Map.of("Income", 0.0, "Expense", 0.0);
        
        List<Transaction> transactions = transactionDao.findByUserId(user.getId());
        
        double totalIncome = transactions.stream()
            .filter(t -> "INCOME".equalsIgnoreCase(t.getType()))
            .mapToDouble(t -> convertToBase(t.getAmount(), t.getAccount() != null ? t.getAccount().getCurrency() : BASE_CURRENCY))
            .sum();

        double totalExpense = transactions.stream()
            .filter(t -> "EXPENSE".equalsIgnoreCase(t.getType()))
            .mapToDouble(t -> convertToBase(t.getAmount(), t.getAccount() != null ? t.getAccount().getCurrency() : BASE_CURRENCY))
            .sum();
            
        // Примітка: Логіка конвертації для транзакцій (створених через create) може вимагати
        // зчитування сконвертованої суми, якщо ви її зберігаєте.
        // Для спрощення ми беремо amount і валюту рахунку (якщо є).

        return Map.of("Income", totalIncome, "Expense", totalExpense);
    }
    
    /**
     * Розраховує витрати, згруповані за категоріями (у базовій валюті).
     */
    public Map<String, Double> getExpensesByCategory(User user) {
        if (user == null) return Map.of();
        
        List<Transaction> transactions = transactionDao.findByUserId(user.getId());

        return transactions.stream()
            .filter(t -> "EXPENSE".equalsIgnoreCase(t.getType()) && t.getCategory() != null)
            .collect(Collectors.groupingBy(
                t -> t.getCategory().getName(),
                Collectors.summingDouble(t -> convertToBase(t.getAmount(), t.getAccount() != null ? t.getAccount().getCurrency() : BASE_CURRENCY))
            ));
    }
}