package ua.kpi.personal.processor;

import ua.kpi.personal.model.Account;
import ua.kpi.personal.model.Goal;
import ua.kpi.personal.model.Transaction;

// Базовий абстрактний клас для всіх декораторів
public abstract class TransactionDecorator implements TransactionProcessor {

    protected final TransactionProcessor wrappedProcessor;

    public TransactionDecorator(TransactionProcessor wrappedProcessor) {
        this.wrappedProcessor = wrappedProcessor;
    }

    @Override
    public Transaction create(Transaction tx) {
        return wrappedProcessor.create(tx);
    }

    @Override
    public Transaction update(Transaction originalTx, Transaction updatedTx) {
        return wrappedProcessor.update(originalTx, updatedTx);
    }

    // ВИПРАВЛЕНО: Рядки 26, 28 - Метод delete повинен приймати Long та делегувати виклик
    @Override
    public void delete(Long transactionId) {
        wrappedProcessor.delete(transactionId);
    }

    @Override
    public void transferToGoal(Account sourceAccount, Goal targetGoal, double amount) {
        wrappedProcessor.transferToGoal(sourceAccount, targetGoal, amount);
    }
}