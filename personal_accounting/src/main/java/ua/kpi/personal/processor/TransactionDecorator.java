package ua.kpi.personal.processor;

import ua.kpi.personal.model.Account;
import ua.kpi.personal.model.Goal;
import ua.kpi.personal.model.Transaction;

public abstract class TransactionDecorator implements TransactionProcessor {

    protected TransactionProcessor wrappedProcessor;

    public TransactionDecorator(TransactionProcessor wrappedProcessor) {
        this.wrappedProcessor = wrappedProcessor;
    }

    @Override
    public Transaction create(Transaction tx) {
        return wrappedProcessor.create(tx);
    }
    
    // ? ЗМІНА: Оновлюємо сигнатуру для прокидання обох транзакцій
    @Override
    public Transaction update(Transaction originalTx, Transaction updatedTx) {
        return wrappedProcessor.update(originalTx, updatedTx);
    }
    
    @Override
    public void delete(Transaction tx) {
        wrappedProcessor.delete(tx);
    }
    
    @Override
    public void transferToGoal(Account sourceAccount, Goal targetGoal, double amount) {
        wrappedProcessor.transferToGoal(sourceAccount, targetGoal, amount);
    }
}