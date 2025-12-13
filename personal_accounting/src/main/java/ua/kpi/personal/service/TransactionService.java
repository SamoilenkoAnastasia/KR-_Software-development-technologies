package ua.kpi.personal.service;

import ua.kpi.personal.model.Transaction;
import ua.kpi.personal.model.analytics.ReportParams;
import ua.kpi.personal.processor.TransactionProcessor;
import ua.kpi.personal.repo.TransactionDao;
import ua.kpi.personal.state.ApplicationSession;
import ua.kpi.personal.state.BudgetAccessState;
import java.util.List;
import java.util.Objects;

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


    private Long validateViewAccessAndGetBudgetId() {
        if (!session.getCurrentBudgetAccessState().canViewBudget()) {
            throw new SecurityException("Помилка: Недостатньо прав для перегляду даних/звітів.");
        }
        return validateAndGetBudgetId();
    }


    private void setTransactionContext(Transaction tx) {
        tx.setBudgetId(validateAndGetBudgetId());
        if (session.getCurrentUser() != null) {
            
            tx.setUser(session.getCurrentUser()); 
            tx.setCreatedBy(session.getCurrentUser()); 
            
            tx.setUserId(session.getCurrentUser().getId()); 
        } else {
            throw new IllegalStateException("Помилка: Користувач не авторизований.");
        }
    }


    public Transaction saveTransaction(Transaction tx) {
        BudgetAccessState state = session.getCurrentBudgetAccessState();
        
        if (!state.canAddTransaction()) {
            throw new SecurityException("Помилка: Недостатньо прав (Add) для додавання транзакцій у цей бюджет.");
        }

        setTransactionContext(tx);

        if (tx.getId() != null) {
            throw new IllegalArgumentException("Використовуйте updateTransaction для оновлення існуючих транзакцій.");
        }
        
        Transaction savedTx = transactionProcessor.create(tx);

        if (savedTx == null || savedTx.getId() == null) {
            throw new RuntimeException("Помилка збереження транзакції: TransactionProcessor/DAO повернув NULL або об'єкт без ID.");
        }
        
        return savedTx;
    }
    

    public Transaction updateTransaction(Transaction originalTx, Transaction updatedTx) {
        Objects.requireNonNull(originalTx.getId(), "Оригінальна транзакція повинна мати ID.");
        
        BudgetAccessState state = session.getCurrentBudgetAccessState();
        Long currentBudgetId = validateAndGetBudgetId();

        if (!state.canModifyFinancialData()) {
            throw new SecurityException("Помилка: Недостатньо прав (Modify) для редагування транзакцій у цьому бюджеті.");
        }

        if (!originalTx.getBudgetId().equals(currentBudgetId)) {
            throw new SecurityException("Помилка: Спроба оновити транзакцію, що не належить активному бюджету.");
        }

        setTransactionContext(updatedTx);
        
        updatedTx.setId(originalTx.getId());
        
        Transaction updatedTxResult = transactionProcessor.update(originalTx, updatedTx);
        
        if (updatedTxResult == null) {
            throw new RuntimeException("Помилка оновлення транзакції: TransactionProcessor/DAO повернув NULL.");
        }
        
        return updatedTxResult;
    }


    public void deleteTransaction(Transaction tx) {
        Objects.requireNonNull(tx.getId(), "Транзакція повинна мати ID для видалення.");
        
        BudgetAccessState state = session.getCurrentBudgetAccessState();
        Long currentBudgetId = validateAndGetBudgetId();

        if (!state.canModifyFinancialData()) {
            throw new SecurityException("Помилка: Недостатньо прав (Modify) для видалення транзакцій.");
        }

        if (!tx.getBudgetId().equals(currentBudgetId)) {
            throw new SecurityException("Помилка: Спроба видалити транзакцію, що не належить активному бюджету.");
        }

        transactionProcessor.delete(tx.getId());
    }
    
    public List<Transaction> getTransactionsByBudgetId() {
        Long currentBudgetId = validateViewAccessAndGetBudgetId(); 
        return transactionDao.findByBudgetId(currentBudgetId);
    }


    public List<Transaction> getTransactionsByDateRange(ReportParams params) {
        Long currentBudgetId = validateViewAccessAndGetBudgetId();
        return transactionDao.findTransactionsByDateRange(params, currentBudgetId);
    }

    
    public List<Object[]> getMonthlySummary(ReportParams params) {
        Long budgetId = validateViewAccessAndGetBudgetId();
        return transactionDao.aggregateMonthlySummary(params, budgetId);
    }
    
    public List<Object[]> getCategorySummary(ReportParams params) {
        Long budgetId = validateViewAccessAndGetBudgetId();
        return transactionDao.aggregateByCategorySummary(params, budgetId);
    }
    

    public TransactionProcessor getTransactionProcessor() {
        return this.transactionProcessor;
    }
    
    public TransactionDao getTransactionDao() {
        return this.transactionDao;
    }
}