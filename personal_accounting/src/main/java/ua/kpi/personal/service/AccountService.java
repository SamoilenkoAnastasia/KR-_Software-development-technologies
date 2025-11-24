package ua.kpi.personal.service;

import ua.kpi.personal.model.Account;
import ua.kpi.personal.model.User; // Потрібно імпортувати, якщо не імпортовано
import ua.kpi.personal.repo.AccountDao;
import ua.kpi.personal.state.ApplicationSession;
import ua.kpi.personal.state.BudgetAccessState;
import java.sql.Connection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class AccountService {

    private final AccountDao accountDao;
    private final ApplicationSession session;

    public AccountService(AccountDao accountDao, ApplicationSession session) {
        this.accountDao = accountDao;
        this.session = session;
    }

    // --- БЕЗПЕЧНІ ОПЕРАЦІЇ ПЕРЕГЛЯДУ ---

    public List<Account> getAccountsForCurrentBudget() {
        Long budgetId = session.getCurrentBudgetId();
        BudgetAccessState accessState = session.getCurrentBudgetAccessState();

        if (budgetId == null || !accessState.canViewBudget()) {
            return List.of();
        }

        List<Account> allAccounts = accountDao.findByBudgetId(budgetId);

        if (accessState.isOwner()) {
            return allAccounts;
        } else {
            // Гість бачить лише СПІЛЬНІ рахунки для перегляду (якщо не потрібен повний список)
            return allAccounts.stream()
                .filter(Account::isShared)
                .collect(Collectors.toList());
        }
    }

    /**
     * Повертає список рахунків, доступних поточному користувачу для
     * створення/редагування транзакцій.
     * Власник бачить усі рахунки бюджету.
     * Гість (редактор) бачить лише СПІЛЬНІ рахунки та СВОЇ особисті рахунки
     * (якщо вони прив'язані до цього бюджету).
     */
    public List<Account> getAccessibleAccountsForTransactions() {
        Long budgetId = session.getCurrentBudgetId();
        User currentUser = session.getCurrentUser();
        BudgetAccessState accessState = session.getCurrentBudgetAccessState();

        if (budgetId == null || currentUser == null || !accessState.canEdit()) {
            // Немає бюджету, користувача або прав редагування
            return List.of();
        }

        Long currentUserId = currentUser.getId();

        // Отримуємо всі рахунки, прив'язані до поточного бюджету
        List<Account> allAccounts = accountDao.findByBudgetId(budgetId);

        if (accessState.isOwner()) {
            // Власник бюджету бачить УСІ рахунки
            return allAccounts;
        } else {
            // Гість (редактор)
            return allAccounts.stream()
                .filter(account -> 
                    // Рахунок спільний АБО
                    account.isShared() || 
                    // Користувач є власником цього рахунку
                    (account.getUser() != null && Objects.equals(account.getUser().getId(), currentUserId))
                )
                .collect(Collectors.toList());
        }
    }


    // --- ОПЕРАЦІЇ СТВОРЕННЯ (Без змін) ---

    public Account createAccount(Account account) {
        BudgetAccessState accessState = session.getCurrentBudgetAccessState();
        
        if (!accessState.canEdit()) {
            throw new SecurityException("Недостатньо прав для створення рахунку.");
        }
        
        // 1. Прив'язка до поточного бюджету
        account.setBudgetId(session.getCurrentBudgetId());
        
        // 2. Встановлення власника
        account.setUser(session.getCurrentUser());
        
        // !!! КЛЮЧОВА ПЕРЕВІРКА БЕЗПЕКИ !!!
        // Якщо користувач НЕ ВЛАСНИК, він НЕ МОЖЕ створити спільний рахунок,
        // незалежно від того, що прийшло з UI.
        if (!accessState.isOwner() && account.isShared()) {
             account.setShared(false);
             // Можливо, тут варто логувати, що спроба створити спільний рахунок була відхилена.
        }
        // Якщо господар, його вибір isShared зберігається.
        
        Objects.requireNonNull(account.getBudgetId(), "Активний Budget ID не встановлено.");
        
        return accountDao.create(account);
    }
    
    public void updateBalanceTransactional(Account account, Connection connection) {
        // У цьому методі ми не перевіряємо права доступу, оскільки він викликається
        // з JdbcTransactionProcessor, де вже буде перевірятися право на ТРАНЗАКЦІЮ.
        try {
            accountDao.update(account, connection);
        } catch (Exception e) {
            throw new RuntimeException("Помилка оновлення балансу рахунку ID " + account.getId(), e);
        }
    }
    
    public boolean checkAccountAccess(Account account) {
         if (account == null) return false;
        
         Long currentUserId = session.getCurrentUser().getId();
         BudgetAccessState accessState = session.getCurrentBudgetAccessState();

         // 1. Власник рахунку завжди має доступ
         if (Objects.equals(account.getUser().getId(), currentUserId)) {
             return true;
         }
        
         if (account.isShared() && accessState.canEdit()) {
             return true;
         }
        
         return false;
    }
}