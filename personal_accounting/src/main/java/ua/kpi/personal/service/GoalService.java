package ua.kpi.personal.service;

import ua.kpi.personal.model.Account;
import ua.kpi.personal.model.Goal;
import ua.kpi.personal.model.User;
import ua.kpi.personal.repo.AccountDao;
import ua.kpi.personal.repo.GoalDao;
import ua.kpi.personal.processor.TransactionProcessor; 
import ua.kpi.personal.state.ApplicationSession;
import java.util.List;

public class GoalService {
    
    private final GoalDao goalDao;
    private final AccountDao accountDao;
    private final TransactionProcessor transactionProcessor; 
    
    public GoalService(GoalDao goalDao, AccountDao accountDao, TransactionProcessor transactionProcessor) {
        this.goalDao = goalDao;
        this.accountDao = accountDao;
        this.transactionProcessor = transactionProcessor;
    }

    public Goal createGoal(Goal goal, User user) {

        Long currentBudgetId = ApplicationSession.getInstance().getCurrentBudgetId();
        goal.setBudgetId(currentBudgetId);
        
        if (goal.getTargetAmount() <= 0) {
             throw new IllegalArgumentException("Цільова сума має бути додатною.");
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

        transactionProcessor.transferToGoal(account, goal, amount); 
    }
    
    public List<Goal> getAllGoals(User user) {
        Long currentBudgetId = ApplicationSession.getInstance().getCurrentBudgetId();
        return goalDao.findByBudgetId(currentBudgetId);
    }
}