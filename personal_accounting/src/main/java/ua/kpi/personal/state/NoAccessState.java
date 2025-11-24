package ua.kpi.personal.state;

public class NoAccessState implements BudgetAccessState {

    @Override
    public boolean canViewBudget() { return false; }

    @Override
    public boolean canAddTransaction() { return false; }

    @Override
    public boolean canModifyFinancialData() { return false; }

    @Override
    public boolean canManageUsers() { return false; }

    @Override
    public boolean canDeleteBudget() { return false; }

    @Override
    public String getDisplayRole() { return "Немає Доступу"; }

}