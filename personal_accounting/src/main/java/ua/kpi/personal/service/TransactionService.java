package ua.kpi.personal.service;

import ua.kpi.personal.model.Transaction;
import ua.kpi.personal.model.analytics.ReportParams;
import ua.kpi.personal.processor.TransactionProcessor;
import ua.kpi.personal.repo.TransactionDao;
import ua.kpi.personal.state.ApplicationSession;
import ua.kpi.personal.state.BudgetAccessState;
import java.util.List;

public class TransactionService {

    private final TransactionProcessor transactionProcessor; 
    private final TransactionDao transactionDao;
    private final ApplicationSession session;

    public TransactionService(TransactionDao dao, TransactionProcessor processor, ApplicationSession session) {
        this.transactionDao = dao;
        this.transactionProcessor = processor;
        this.session = session;
    }

    private Long validateAndGetBudgetId() {
        Long currentBudgetId = session.getCurrentBudgetId();
        if (currentBudgetId == null) {
            throw new IllegalStateException("Помилка: Не обрано активний бюджет.");
        }
        return currentBudgetId;
    }

    private void setTransactionContext(Transaction tx) {
        tx.setBudgetId(validateAndGetBudgetId());
        if (session.getCurrentUser() != null) {
            // Встановлює власника транзакції (це може бути власник рахунку або просто user_id)
            tx.setUser(session.getCurrentUser()); 
            
            // !!! НОВЕ: Встановлює фактичного творця транзакції !!!
            tx.setCreatedBy(session.getCurrentUser()); 
        }
    }


    public Transaction saveTransaction(Transaction tx) {
        BudgetAccessState state = session.getCurrentBudgetAccessState();
        
        if (!state.canAddTransaction()) {
            throw new SecurityException("Помилка: Недостатньо прав (Add) для додавання транзакцій у цей бюджет.");
        }

        // Встановлення budgetId, user та createdBy
        setTransactionContext(tx);

        if (tx.getId() != null) {
             throw new IllegalArgumentException("Використовуйте updateTransaction для оновлення існуючих транзакцій.");
        }
        
        return transactionProcessor.create(tx);
    }
    

    public Transaction updateTransaction(Transaction originalTx, Transaction updatedTx) {
        BudgetAccessState state = session.getCurrentBudgetAccessState();
        Long currentBudgetId = validateAndGetBudgetId();

        if (!state.canModifyFinancialData()) {
            throw new SecurityException("Помилка: Недостатньо прав (Modify) для редагування транзакцій у цьому бюджеті.");
        }

        if (originalTx.getBudgetId() == null || !originalTx.getBudgetId().equals(currentBudgetId)) {
              throw new SecurityException("Помилка: Спроба оновити транзакцію, що не належить активному бюджету.");
        }

        setTransactionContext(updatedTx);
        
        return transactionProcessor.update(originalTx, updatedTx);
    }


    public void deleteTransaction(Transaction tx) {
        BudgetAccessState state = session.getCurrentBudgetAccessState();
        Long currentBudgetId = validateAndGetBudgetId();

        if (!state.canModifyFinancialData()) {
            throw new SecurityException("Помилка: Недостатньо прав (Modify) для видалення транзакцій.");
        }

        if (tx.getBudgetId() == null || !tx.getBudgetId().equals(currentBudgetId)) {
              throw new SecurityException("Помилка: Спроба видалити транзакцію, що не належить активному бюджету.");
        }

        // РЯДОК 88: ВИПРАВЛЕНО - виклик void
        transactionProcessor.delete(tx.getId());
    }
    
    // ... (інші методи) ...

    public List<Transaction> getTransactionsByBudgetId(Long budgetId) {
        BudgetAccessState state = session.getCurrentBudgetAccessState();
        if (!state.canViewBudget()) {
            // Приховуємо дані, якщо немає прав
            System.err.println("Помилка: Недостатньо прав для перегляду транзакцій.");
            throw new SecurityException("Недостатньо прав для перегляду транзакцій.");
        }

        Long currentBudgetId = validateAndGetBudgetId(); 

        if (!currentBudgetId.equals(budgetId)) {
              throw new IllegalArgumentException("Запитуваний ID бюджету не відповідає активному.");
        }
   
        return transactionDao.findByBudgetId(currentBudgetId);
    }


    public List<Transaction> getTransactionsByDateRange(ReportParams params) {
        BudgetAccessState state = session.getCurrentBudgetAccessState();
        if (!state.canViewBudget()) {
            // Приховуємо дані, якщо немає прав
              throw new SecurityException("Помилка: Недостатньо прав для перегляду звітів.");
        }

        Long currentBudgetId = validateAndGetBudgetId();

        return transactionDao.findTransactionsByDateRange(params, currentBudgetId);
    }

    
    public List<Object[]> getMonthlySummary(ReportParams params) {
        if (!session.getCurrentBudgetAccessState().canViewBudget()) {
              throw new SecurityException("Помилка: Недостатньо прав для перегляду звітів.");
        }
        Long budgetId = validateAndGetBudgetId();
        return transactionDao.aggregateMonthlySummary(params, budgetId);
    }
    
    public List<Object[]> getCategorySummary(ReportParams params) {
        if (!session.getCurrentBudgetAccessState().canViewBudget()) {
              throw new SecurityException("Помилка: Недостатньо прав для перегляду звітів.");
        }
        Long budgetId = validateAndGetBudgetId();
        return transactionDao.aggregateByCategorySummary(params, budgetId);
    }
    

    public TransactionProcessor getTransactionProcessor() {
        return this.transactionProcessor;
    }
    
    public TransactionDao getTransactionDao() {
        return this.transactionDao;
    }
}
