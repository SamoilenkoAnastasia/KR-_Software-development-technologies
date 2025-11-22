package ua.kpi.personal.processor;

import ua.kpi.personal.model.Transaction;
import ua.kpi.personal.model.Goal;
import ua.kpi.personal.model.Account;

public interface TransactionProcessor { 
    
    Transaction create(Transaction tx);
    
    // ? ЗМІНА: Метод update тепер приймає стару (originalTx) та нову (updatedTx) версії транзакції.
    Transaction update(Transaction originalTx, Transaction updatedTx); 
    
    void delete(Transaction tx); 
    
    void transferToGoal(Account sourceAccount, Goal targetGoal, double amount);
}