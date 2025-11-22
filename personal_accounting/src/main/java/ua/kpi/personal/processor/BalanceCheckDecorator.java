package ua.kpi.personal.processor;

import ua.kpi.personal.model.Account;
import ua.kpi.personal.model.Goal;
import ua.kpi.personal.model.Transaction;

public class BalanceCheckDecorator extends TransactionDecorator {

    public BalanceCheckDecorator(TransactionProcessor wrappedProcessor) {
        super(wrappedProcessor);
    }

    @Override
    public Transaction create(Transaction tx) {
        if ("EXPENSE".equals(tx.getType())) {
            checkBalanceForExpense(tx.getAccount(), tx.getAmount());
            System.out.println("BalanceCheckDecorator: Баланс OK для CREATE. Продовжуємо обробку.");
        } 
        return super.create(tx);
    }

    // ? НОВИЙ/ОНОВЛЕНИЙ МЕТОД UPDATE для перевірки балансу
    @Override
    public Transaction update(Transaction originalTx, Transaction updatedTx) {
        // Якщо рахунок змінився, або тип транзакції змінився
        if (!originalTx.getAccount().getId().equals(updatedTx.getAccount().getId()) || 
            !originalTx.getType().equals(updatedTx.getType())) {
            
            // Якщо нова транзакція - це Витрата, перевіряємо, чи вистачить коштів на новому рахунку
            if ("EXPENSE".equals(updatedTx.getType())) {
                checkBalanceForExpense(updatedTx.getAccount(), updatedTx.getAmount());
            }
            
            // Ми не можемо перевірити баланс після відкату старого ефекту, 
            // оскільки не знаємо його тут. Припускаємо, що це зробить TransactionDao, 
            // або що загальний баланс OK, якщо перевірка вище пройшла.
            
        } else if ("EXPENSE".equals(updatedTx.getType())) {
            // Якщо це Витрата, і рахунок/тип не змінився:
            // Перевіряємо, чи нова сума (updatedTx) більша за стару (originalTx)
            // і чи різниця не перевищує доступний баланс.
            double oldAmount = originalTx.getAmount();
            double newAmount = updatedTx.getAmount();
            
            if (newAmount > oldAmount) {
                double amountDifference = newAmount - oldAmount;
                checkBalanceForExpense(updatedTx.getAccount(), amountDifference);
            }
        }
        
        System.out.println("BalanceCheckDecorator: Баланс OK для UPDATE. Продовжуємо обробку.");
        return super.update(originalTx, updatedTx);
    }
    
    // Приватний метод для централізованої перевірки (створено для чистоти)
    private void checkBalanceForExpense(Account account, double amountToCheck) {
        double currentBalance = account.getBalance() != null ? account.getBalance() : 0.0;
        
        if (currentBalance < amountToCheck) {
            String errorMessage = String.format(
                "Помилка: Недостатньо коштів на рахунку '%s'. Поточний баланс: %.2f %s, необхідна додаткова сума: %.2f %s.",
                account.getName(), 
                currentBalance, account.getCurrency(), 
                amountToCheck, account.getCurrency()
            );
            throw new RuntimeException(errorMessage); 
        }
    }
    
    // Решта успадкованих методів (delete, transferToGoal) прокидаються до wrappedProcessor.
}