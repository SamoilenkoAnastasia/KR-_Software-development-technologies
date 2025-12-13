package ua.kpi.personal.service;

import ua.kpi.personal.model.Account;
import ua.kpi.personal.model.Category;
import ua.kpi.personal.model.Goal;
import ua.kpi.personal.model.User;
import ua.kpi.personal.repo.AccountDao;
import ua.kpi.personal.repo.CategoryDao; 
import ua.kpi.personal.repo.GoalDao;
import ua.kpi.personal.processor.TransactionProcessor;
import ua.kpi.personal.state.ApplicationSession;

import java.util.List;

public class GoalService {
    
    private final GoalDao goalDao;
    private final AccountDao accountDao;
    private final CategoryDao categoryDao; 
    private final TransactionProcessor transactionProcessor;
    
    public GoalService(GoalDao goalDao, AccountDao accountDao, CategoryDao categoryDao, TransactionProcessor transactionProcessor) {
        this.goalDao = goalDao;
        this.accountDao = accountDao;
        this.categoryDao = categoryDao; 
        this.transactionProcessor = transactionProcessor;
    }

  
    public Goal createGoal(Goal goal, User user) {
        Long currentBudgetId = ApplicationSession.getInstance().getCurrentBudgetId();
        
        if (currentBudgetId == null) {
              throw new IllegalStateException("Активний бюджет не визначено.");
        }
       
        goal.setBudgetId(currentBudgetId);
        goal.setUserId(user.getId()); 
        
        if (goal.getTargetAmount() <= 0) {
            throw new IllegalArgumentException("Цільова сума має бути додатною.");
        }
        
        String categoryName = "Накопичення: " + goal.getName();
        Category newGoalCategory = new Category(user.getId(), categoryName, "EXPENSE", null); 

        try {
            newGoalCategory = categoryDao.create(newGoalCategory);
            goal.setCategoryId(newGoalCategory.getId());
            
        } catch (Exception e) {
            throw new RuntimeException("Помилка при створенні категорії для цілі.", e);
        }
        
        return goalDao.create(goal);
    }

    public void contributeToGoal(Long goalId, Long accountId, double amount, User user) {
        Long currentBudgetId = ApplicationSession.getInstance().getCurrentBudgetId();
    
        Goal goal = goalDao.findById(goalId, currentBudgetId);
        Account account = accountDao.findById(accountId, user.getId());

        if (goal == null) {
            throw new IllegalArgumentException("Ціль не знайдена або ви не маєте до неї доступу.");
        }
        if (account == null) {
            throw new IllegalArgumentException("Рахунок не знайдений.");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Сума внеску має бути додатною.");
        }
        
        Long categoryId = goal.getCategoryId();
        if (categoryId == null) {
              throw new IllegalStateException("Ціль не має прив'язаної Category ID.");
        }

        transactionProcessor.transferToGoal(account, goal, amount, categoryId); 

        double newCollectedAmount = goal.getCurrentAmount() + amount;
        
        if (newCollectedAmount > goal.getTargetAmount()) {
            newCollectedAmount = goal.getTargetAmount();
        }
        
        goal.setCurrentAmount(newCollectedAmount);
        goalDao.update(goal);         
    }

    public void deleteGoal(Long goalId, User user) {
        Long currentBudgetId = ApplicationSession.getInstance().getCurrentBudgetId();
        
        Goal goal = goalDao.findById(goalId, currentBudgetId);
        
        if (goal == null) {
            throw new IllegalArgumentException("Ціль не знайдена.");
        }
        
        goalDao.delete(goal.getId(), goal.getBudgetId()); 

        Long categoryId = goal.getCategoryId();
        if (categoryId != null) {
            try {
                categoryDao.delete(categoryId);
            } catch (Exception e) {
                System.err.println("Помилка при видаленні категорії цілі ID " + categoryId + ": " + e.getMessage());
            }
        }
    }

 
    public List<Goal> getAllGoals(User user) {
        Long currentBudgetId = ApplicationSession.getInstance().getCurrentBudgetId();
        return goalDao.findByBudgetId(currentBudgetId);
    }
}