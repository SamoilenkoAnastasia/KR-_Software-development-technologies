package ua.kpi.personal.state;

public class OwnerState implements BudgetAccessState {

    @Override
    public boolean canViewBudget() { return true; }

    @Override
    public boolean canAddTransaction() { return true; }

    @Override
    public boolean canModifyFinancialData() { return true; }

    @Override
    public boolean canManageUsers() { return true; }

    @Override
    public boolean canDeleteBudget() { return true; }

    @Override
    public String getDisplayRole() { return "Власник"; }
}