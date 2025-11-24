package ua.kpi.personal.service;

import ua.kpi.personal.model.Account;
import ua.kpi.personal.model.User;
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
            return allAccounts.stream()
                .filter(Account::isShared)
                .collect(Collectors.toList());
        }
    }

    public List<Account> getAccessibleAccountsForTransactions() {
        Long budgetId = session.getCurrentBudgetId();
        User currentUser = session.getCurrentUser();
        BudgetAccessState accessState = session.getCurrentBudgetAccessState();

        if (budgetId == null || currentUser == null || !accessState.canEdit()) {
            return List.of();
        }

        Long currentUserId = currentUser.getId();

        List<Account> allAccounts = accountDao.findByBudgetId(budgetId);

        if (accessState.isOwner()) {
            return allAccounts;
        } else {
            return allAccounts.stream()
                .filter(account ->
                    account.isShared() ||
                    (account.getUser() != null && Objects.equals(account.getUser().getId(), currentUserId))
                )
                .collect(Collectors.toList());
        }
    }

    public Account createAccount(Account account) {
        BudgetAccessState accessState = session.getCurrentBudgetAccessState();

        if (!accessState.canEdit()) {
            throw new SecurityException("Недостатньо прав для створення рахунку.");
        }

        account.setBudgetId(session.getCurrentBudgetId());

        account.setUser(session.getCurrentUser());

        if (!accessState.isOwner() && account.isShared()) {
            account.setShared(false);
        }

        Objects.requireNonNull(account.getBudgetId(), "Активний Budget ID не встановлено.");

        return accountDao.create(account);
    }

    public void updateBalanceTransactional(Account account, Connection connection) {
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

        if (Objects.equals(account.getUser().getId(), currentUserId)) {
            return true;
        }

        if (account.isShared() && accessState.canEdit()) {
            return true;
        }

        return false;
    }
}