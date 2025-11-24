package ua.kpi.personal.processor;

import ua.kpi.personal.model.Account;
import ua.kpi.personal.model.Transaction;
import ua.kpi.personal.repo.AccountDao;
import ua.kpi.personal.repo.TransactionDao;
import ua.kpi.personal.service.AccountService;
import ua.kpi.personal.model.Goal;
import ua.kpi.personal.util.Db;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

public class JdbcTransactionProcessor implements TransactionProcessor {
    
    private final TransactionDao transactionDao;
    private final AccountDao accountDao;
    private final AccountService accountService;

    public JdbcTransactionProcessor(TransactionDao transactionDao, AccountDao accountDao, AccountService accountService) {
        this.transactionDao = transactionDao;
        this.accountDao = accountDao;
        this.accountService = accountService;
    }

    
    @Override
    public Transaction create(Transaction tx) {
        Connection c = null;
        try {
            c = Db.getConnection();
            c.setAutoCommit(false); // Початок транзакції
          
            Account account = accountDao.findById(tx.getAccountId());
            if (account == null) {
                throw new IllegalArgumentException("Рахунок ID " + tx.getAccountId() + " не знайдено.");
            }
            
            // !!! НОВА ПЕРЕВІРКА ДОСТУПУ !!!
            // Перевіряємо, чи має поточний користувач право використовувати цей рахунок для транзакції
            if (!accountService.checkAccountAccess(account)) {
                throw new SecurityException("Поточний користувач не має прав на використання рахунку ID " + tx.getAccountId());
            }
            // !!! КІНЕЦЬ ПЕРЕВІРКИ !!!
            
            double amount = tx.getAmount();
            double balanceChange = tx.isIncome() ? amount : -amount;
            
            account.setBalance(account.getBalance() + balanceChange);
            accountService.updateBalanceTransactional(account, c);

            
            transactionDao.create(tx, c); 

            c.commit(); // Завершення транзакції
            return tx;

        } catch (Exception e) {
            System.err.println("Помилка при створенні транзакції. Спроба відкату.");
            Db.rollback(c); // Відкат у разі помилки
            throw new RuntimeException("Неможливо створити транзакцію та оновити баланс: " + e.getMessage(), e);
        } finally {
            Db.close(c); // Закриваємо з'єднання
        }
    }

    // 2. МЕТОД UPDATE
    @Override
    public Transaction update(Transaction originalTx, Transaction updatedTx) {
        Connection c = null;
        try {
            Objects.requireNonNull(originalTx, "Original transaction must not be null.");
            
            c = Db.getConnection();
            c.setAutoCommit(false); // Початок транзакції
            
            // !!! НОВА ПЕРЕВІРКА ДОСТУПУ !!!
            // Перевіряємо доступ до старого рахунку та нового рахунку
            Account originalAccount = accountDao.findById(originalTx.getAccountId());
            Account updatedAccount = accountDao.findById(updatedTx.getAccountId());

            if (!accountService.checkAccountAccess(originalAccount)) {
                 throw new SecurityException("Немає прав на зміну оригінального рахунку ID " + originalTx.getAccountId());
            }
            if (!accountService.checkAccountAccess(updatedAccount)) {
                 throw new SecurityException("Немає прав на використання нового рахунку ID " + updatedTx.getAccountId());
            }
            // !!! КІНЕЦЬ ПЕРЕВІРКИ !!!
            
            // 1. Відкат балансу від оригінальної транзакції
            revertBalance(originalTx, c); 
            
            // 2. Застосування нового балансу від оновленої транзакції
            applyBalance(updatedTx, c); 
            
            // 3. Оновлення самої транзакції
            transactionDao.update(originalTx, updatedTx, c);
            
            c.commit(); // Завершення транзакції
            return updatedTx;
        } catch (Exception e) {
            System.err.println("Помилка при оновленні транзакції. Спроба відкату.");
            Db.rollback(c); // Відкат у разі помилки
            throw new RuntimeException("Неможливо оновити транзакцію та баланси: " + e.getMessage(), e);
        } finally {
            Db.close(c); // Закриваємо з'єднання
        }
    }

    
    // 3. МЕТОД DELETE
    @Override
    public void delete(Long transactionId) {
        Connection c = null;
        try {
            c = Db.getConnection();
            c.setAutoCommit(false); // Початок транзакції
            
            // Отримуємо транзакцію для відкату балансу (потрібен budgetId, але тут 0L - припускаємо, що він буде знайдений)
            Transaction txToDelete = transactionDao.findById(transactionId, 0L); 
            
            if (txToDelete == null) {
                 throw new IllegalArgumentException("Транзакція ID " + transactionId + " не знайдена.");
            }
            
            // !!! НОВА ПЕРЕВІРКА ДОСТУПУ !!!
            Account account = accountDao.findById(txToDelete.getAccountId());
            if (!accountService.checkAccountAccess(account)) {
                 throw new SecurityException("Немає прав на видалення транзакції з приватного/недоступного рахунку ID " + txToDelete.getAccountId());
            }
            // !!! КІНЕЦЬ ПЕРЕВІРКИ !!!

            // Відкат балансу перед видаленням
            revertBalance(txToDelete, c); 
            
            // Видалення самої транзакції
            transactionDao.delete(txToDelete, c); 
            
            c.commit(); // Завершення транзакції
        } catch (Exception e) {
            System.err.println("Помилка при видаленні транзакції. Спроба відкату.");
            Db.rollback(c); // Відкат у разі помилки
            throw new RuntimeException("Неможливо видалити транзакцію та оновити баланс: " + e.getMessage(), e);
        } finally {
            Db.close(c); // Закриваємо з'єднання
        }
    }
    
    // 4. ДОПОМІЖНИЙ МЕТОД: REVERT BALANCE (без змін)
    private void revertBalance(Transaction tx, Connection c) throws Exception {
        Account account = accountDao.findById(tx.getAccountId());
        if (account == null) {
             throw new IllegalArgumentException("Рахунок ID " + tx.getAccountId() + " не знайдено для відкату.");
        }
        
        double amountToRevert = tx.isIncome() ? -tx.getAmount() : tx.getAmount();
        
        account.setBalance(account.getBalance() + amountToRevert);
        accountService.updateBalanceTransactional(account, c);
    }
    
    // 5. ДОПОМІЖНИЙ МЕТОД: APPLY BALANCE (без змін)
    private void applyBalance(Transaction tx, Connection c) throws Exception {
         Account account = accountDao.findById(tx.getAccountId());
        if (account == null) {
             throw new IllegalArgumentException("Рахунок ID " + tx.getAccountId() + " не знайдено для застосування.");
        }
        
        double amountToApply = tx.isIncome() ? tx.getAmount() : -tx.getAmount();
        
        account.setBalance(account.getBalance() + amountToApply);
        accountService.updateBalanceTransactional(account, c);
    }
    
    // 6. МЕТОД TRANSFER TO GOAL (без змін)
    @Override
    public void transferToGoal(Account sourceAccount, Goal targetGoal, double amount) {
        // Реалізація логіки переказу коштів на ціль
    }
}