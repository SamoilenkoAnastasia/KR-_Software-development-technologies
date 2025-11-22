package ua.kpi.personal.processor;

import ua.kpi.personal.controller.TransactionsController;
import ua.kpi.personal.model.Account;
import ua.kpi.personal.model.Goal;
import ua.kpi.personal.model.Transaction;


public class UiNotificationDecorator extends TransactionDecorator {

    private final TransactionsController controller;
    
    public UiNotificationDecorator(TransactionProcessor wrappedProcessor, TransactionsController controller) {
        super(wrappedProcessor);
        this.controller = controller;
    }

    @Override
    public Transaction create(Transaction tx) {
        try {
            Transaction savedTx = super.create(tx);
            String type = savedTx.getType().equals("EXPENSE") ? "Витрату" : "Дохід";
            String successMsg = "? " + type + " успішно збережено!";
            controller.displaySuccessDialog(successMsg); 
            return savedTx;
        } catch (RuntimeException e) {
            String errorMsg = e.getMessage();
            controller.displayErrorDialog(errorMsg);
            throw e; 
        }
    }
    
    @Override
    public Transaction update(Transaction originalTx, Transaction updatedTx) {
        try {
            // Викликає wrappedProcessor.update(originalTx, updatedTx)
            Transaction resultTx = super.update(originalTx, updatedTx); 
            String successMsg = "? Транзакцію ID " + resultTx.getId() + " успішно оновлено!";
            controller.displaySuccessDialog(successMsg);
            return resultTx;
        } catch (RuntimeException e) {
            String errorMsg = e.getMessage();
            controller.displayErrorDialog(errorMsg);
            throw e;
        }
    }
    
    // ? ВИПРАВЛЕНО: Додана обробка для DELETE
    @Override
    public void delete(Transaction tx) {
        try {
            super.delete(tx); // Викликає wrappedProcessor.delete(tx)
            String successMsg = "? Транзакцію ID " + tx.getId() + " успішно видалено!";
            controller.displaySuccessDialog(successMsg);
        } catch (RuntimeException e) {
            String errorMsg = e.getMessage();
            controller.displayErrorDialog(errorMsg);
            throw e;
        }
    }

    @Override
    public void transferToGoal(Account sourceAccount, Goal targetGoal, double amount) {
        // Залишаємо виклик батьківського методу, який просто прокине його далі
        super.transferToGoal(sourceAccount, targetGoal, amount); 
    }
}