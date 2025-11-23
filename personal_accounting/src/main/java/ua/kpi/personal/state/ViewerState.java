package ua.kpi.personal.state;

public class ViewerState implements BudgetAccessState {

    @Override
    public boolean canViewBudget() { return true; }

    /** Переглядач не може додавати транзакції. */
    @Override
    public boolean canAddTransaction() { return false; }

    /** Переглядач не може редагувати чи видаляти фінансові дані. */
    @Override
    public boolean canModifyFinancialData() { return false; }

    @Override
    public boolean canManageUsers() { return false; }

    @Override
    public boolean canDeleteBudget() { return false; }

    @Override
    public String getDisplayRole() { return "Переглядач"; }
}