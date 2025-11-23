package ua.kpi.personal.state;

public class OwnerState implements BudgetAccessState {

    @Override
    public boolean canViewBudget() { return true; }

    @Override
    public boolean canAddTransaction() { return true; }

    /** Власник може редагувати та видаляти будь-які фінансові дані. */
    @Override
    public boolean canModifyFinancialData() { return true; }

    /** Власник може керувати користувачами. */
    @Override
    public boolean canManageUsers() { return true; }

    /** Власник може видалити бюджет. */
    @Override
    public boolean canDeleteBudget() { return true; }

    @Override
    public String getDisplayRole() { return "Власник"; }
}