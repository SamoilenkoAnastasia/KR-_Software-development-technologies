package ua.kpi.personal.state;

public class EditorState implements BudgetAccessState {

    @Override
    public boolean canViewBudget() { return true; }

    @Override
    public boolean canAddTransaction() { return true; }

    @Override
    public boolean canModifyFinancialData() { return true; }

    @Override
    public boolean canManageUsers() { return false; }

    @Override
    public boolean canDeleteBudget() { return false; }

    @Override
    public String getDisplayRole() { return "Редактор"; }
}