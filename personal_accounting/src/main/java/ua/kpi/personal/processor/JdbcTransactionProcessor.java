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
            c.setAutoCommit(false);

            Account account = accountDao.findById(tx.getAccountId());
            if (account == null) {
                throw new IllegalArgumentException("Рахунок ID " + tx.getAccountId() + " не знайдено.");
            }

            if (!accountService.checkAccountAccess(account)) {
                throw new SecurityException("Поточний користувач не має прав на використання рахунку ID " + tx.getAccountId());
            }

            double amount = tx.getAmount();
            double balanceChange = tx.isIncome() ? amount : -amount;

            account.setBalance(account.getBalance() + balanceChange);
            accountService.updateBalanceTransactional(account, c);
            transactionDao.create(tx, c);

            c.commit();
            return tx;

        } catch (Exception e) {
            System.err.println("Помилка при створенні транзакції.");
            Db.rollback(c);
            throw new RuntimeException("Неможливо створити транзакцію та оновити баланс: " + e.getMessage(), e);
        } finally {
            Db.close(c);
        }
    }

    @Override
    public Transaction update(Transaction originalTx, Transaction updatedTx) {
        Connection c = null;
        try {
            Objects.requireNonNull(originalTx, "Оригінальна транзакція не повинна бути нульовою.");

            c = Db.getConnection();
            c.setAutoCommit(false);

            Account originalAccount = accountDao.findById(originalTx.getAccountId());
            Account updatedAccount = accountDao.findById(updatedTx.getAccountId());

            if (!accountService.checkAccountAccess(originalAccount)) {
                throw new SecurityException("Немає прав на зміну оригінального рахунку ID " + originalTx.getAccountId());
            }
            if (!accountService.checkAccountAccess(updatedAccount)) {
                throw new SecurityException("Немає прав на використання нового рахунку ID " + updatedTx.getAccountId());
            }

            revertBalance(originalTx, c);

            applyBalance(updatedTx, c);

            transactionDao.update(originalTx, updatedTx, c);

            c.commit();
            return updatedTx;
        } catch (Exception e) {
            System.err.println("Помилка при оновленні транзакції.");
            Db.rollback(c);
            throw new RuntimeException("Неможливо оновити транзакцію та баланси: " + e.getMessage(), e);
        } finally {
            Db.close(c);
        }
    }


    @Override
    public void delete(Long transactionId) {
        Connection c = null;
        try {
            c = Db.getConnection();
            c.setAutoCommit(false);

            Transaction txToDelete = transactionDao.findById(transactionId, 0L);

            if (txToDelete == null) {
                throw new IllegalArgumentException("Транзакція ID " + transactionId + " не знайдена.");
            }

            Account account = accountDao.findById(txToDelete.getAccountId());
            if (!accountService.checkAccountAccess(account)) {
                throw new SecurityException("Немає прав на видалення транзакції з приватного/недоступного рахунку ID " + txToDelete.getAccountId());
            }

            revertBalance(txToDelete, c);

            transactionDao.delete(txToDelete, c);

            c.commit();
        } catch (Exception e) {
            System.err.println("Помилка при видаленні транзакції. Спроба відкату.");
            Db.rollback(c);
            throw new RuntimeException("Неможливо видалити транзакцію та оновити баланс: " + e.getMessage(), e);
        } finally {
            Db.close(c);
        }
    }

    private void revertBalance(Transaction tx, Connection c) throws Exception {
        Account account = accountDao.findById(tx.getAccountId());
        if (account == null) {
            throw new IllegalArgumentException("Рахунок ID " + tx.getAccountId() + " не знайдено для відкату.");
        }

        double amountToRevert = tx.isIncome() ? -tx.getAmount() : tx.getAmount();

        account.setBalance(account.getBalance() + amountToRevert);
        accountService.updateBalanceTransactional(account, c);
    }

    private void applyBalance(Transaction tx, Connection c) throws Exception {
        Account account = accountDao.findById(tx.getAccountId());
        if (account == null) {
            throw new IllegalArgumentException("Рахунок ID " + tx.getAccountId() + " не знайдено для застосування.");
        }

        double amountToApply = tx.isIncome() ? tx.getAmount() : -tx.getAmount();

        account.setBalance(account.getBalance() + amountToApply);
        accountService.updateBalanceTransactional(account, c);
    }

    @Override
    public void transferToGoal(Account sourceAccount, Goal targetGoal, double amount) {}
}