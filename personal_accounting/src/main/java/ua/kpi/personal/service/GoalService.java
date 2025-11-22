package ua.kpi.personal.service;

import ua.kpi.personal.model.Account;
import ua.kpi.personal.model.Goal;
import ua.kpi.personal.model.User;
import ua.kpi.personal.repo.AccountDao;
import ua.kpi.personal.repo.GoalDao;
import ua.kpi.personal.processor.TransactionProcessor; 
import java.util.List;

public class GoalService {
    
    private final GoalDao goalDao;
    private final AccountDao accountDao;
    private final TransactionProcessor transactionProcessor; 
    
    // Впроваджуємо залежності (DAO та Processor)
    public GoalService(GoalDao goalDao, AccountDao accountDao, TransactionProcessor transactionProcessor) {
        this.goalDao = goalDao;
        this.accountDao = accountDao;
        this.transactionProcessor = transactionProcessor;
    }

    /**
     * Створює нову ціль.
     */
    public Goal createGoal(Goal goal, User user) {
        goal.setUser(user);
        if (goal.getTargetAmount() <= 0) {
             throw new IllegalArgumentException("Цільова сума має бути додатною.");
        }
        return goalDao.create(goal);
    }

    /**
     * Логіка внесення коштів:
     */
    public void contributeToGoal(Long goalId, Long accountId, double amount, User user) {
        // 1. Отримання об'єктів з БД
        Goal goal = goalDao.findById(goalId, user.getId());
        Account account = accountDao.findById(accountId, user.getId()); 

        if (goal == null) {
            throw new IllegalArgumentException("Ціль не знайдена.");
        }
        if (account == null) {
            throw new IllegalArgumentException("Рахунок не знайдений.");
        }
        
        // ? ВИДАЛЕНО: СТРОГА ПЕРЕВІРКА ВАЛЮТ. ЇЇ тепер обробляє CurrencyDecorator.
        /*
        if (!account.getCurrency().equals(goal.getCurrency())) {
             throw new IllegalArgumentException("Валюти рахунку та цілі повинні співпадати!");
        }
        */
        
        // 3. ПЕРЕВІРКА СУМИ (залишаємо лише перевірку на нуль)
        if (amount <= 0) {
             throw new IllegalArgumentException("Сума внеску має бути додатною.");
        }
        
        // 4. Виклик логіки переказу.
        transactionProcessor.transferToGoal(account, goal, amount); 
    }
    
    // Метод для отримання всіх цілей
    public List<Goal> getAllGoals(User user) {
        return goalDao.findByUserId(user.getId());
    }
}