package ua.kpi.personal.processor;

import ua.kpi.personal.model.Account;
import ua.kpi.personal.model.Goal;
import ua.kpi.personal.model.Transaction;
import ua.kpi.personal.repo.AccountDao;
import ua.kpi.personal.repo.TransactionDao;
import ua.kpi.personal.util.Db;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

public class BalanceCheckDecorator extends TransactionDecorator {

    private final AccountDao accountDao;
    private final TransactionDao transactionDao; 

    public BalanceCheckDecorator(TransactionProcessor wrappedProcessor, AccountDao accountDao, TransactionDao transactionDao) {
        super(wrappedProcessor);
        this.accountDao = accountDao;
        this.transactionDao = transactionDao;
    }

    
    private void checkBalanceForExpense(Account account, double amountToCheck) {
        double currentBalance = account.getBalance() != null ? account.getBalance() : 0.0;
        
        if (currentBalance < amountToCheck) {
            String errorMessage = String.format(
                "Помилка: Недостатньо коштів на рахунку '%s' (%s). Поточний баланс: %.2f %s, необхідна сума: %.2f %s.",
                account.getName(), 
                account.getId(),
                currentBalance, account.getCurrency(), 
                amountToCheck, account.getCurrency()
            );
            throw new RuntimeException(errorMessage); 
        }
    }


    @Override
    public Transaction create(Transaction tx) {
        Objects.requireNonNull(tx.getAccount(), "Рахунок не повинен бути null.");
        
        Account account = accountDao.findById(tx.getAccountId());
        if (account == null) {
            throw new IllegalArgumentException("Рахунок ID " + tx.getAccountId() + " не знайдено.");
        }
        
        if ("EXPENSE".equals(tx.getType())) {
            checkBalanceForExpense(account, tx.getAmount());
        }
   
        Transaction savedTx = super.create(tx);
        System.out.println("BalanceCheckDecorator: Успішно створено (перевірено баланс).");
        return savedTx;
    }

    @Override
    public Transaction update(Transaction originalTx, Transaction updatedTx) {
        Objects.requireNonNull(originalTx, "Оригінальна транзакція не повинна бути null.");
        Objects.requireNonNull(updatedTx, "Оновлена транзакція не повинна бути null.");

        if ("EXPENSE".equals(updatedTx.getType())) {
            Account currentAccount = accountDao.findById(updatedTx.getAccountId());
            if (currentAccount == null) {
                throw new IllegalArgumentException("Рахунок ID " + updatedTx.getAccountId() + " не знайдено.");
            }

            if (!originalTx.isIncome() && originalTx.getAccountId().equals(updatedTx.getAccountId())) {
             
                if (currentAccount.getBalance() - updatedTx.getAmount() + originalTx.getAmount() < 0) {
                    checkBalanceForExpense(currentAccount, updatedTx.getAmount()); 
                }
            } else {
                checkBalanceForExpense(currentAccount, updatedTx.getAmount()); 
            }
        }

        Transaction processedTx = super.update(originalTx, updatedTx);
        System.out.println("BalanceCheckDecorator: Успішно оновлено (перевірено баланс).");
        return processedTx;
    }

    @Override
    public void delete(Long transactionId) {
        Transaction tx = transactionDao.findById(transactionId, 0L); 
        
        if (tx == null) {
            super.delete(transactionId); 
            return;
        }

        if (tx.isIncome()) {
            Account account = accountDao.findById(tx.getAccountId());
            if (account == null) {
                throw new IllegalArgumentException("Рахунок ID " + tx.getAccountId() + " не знайдено.");
            }

            checkBalanceForExpense(account, tx.getAmount());
        }
        
        super.delete(transactionId);
        System.out.println("BalanceCheckDecorator: Успішно видалено (перевірено баланс).");
    }

    @Override
    public void transferToGoal(Account sourceAccount, Goal targetGoal, double amount) {
        checkBalanceForExpense(sourceAccount, amount);
        super.transferToGoal(sourceAccount, targetGoal, amount); 
    }
}
