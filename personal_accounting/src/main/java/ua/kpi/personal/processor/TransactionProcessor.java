package ua.kpi.personal.processor;

import ua.kpi.personal.model.Transaction;
import ua.kpi.personal.model.Goal;
import ua.kpi.personal.model.Account;

public interface TransactionProcessor { 
    
    Transaction create(Transaction tx);
    
    Transaction update(Transaction originalTx, Transaction updatedTx); 
    
    void delete(Long transactionId);
    
    void transferToGoal(Account sourceAccount, Goal targetGoal, double amount);
}