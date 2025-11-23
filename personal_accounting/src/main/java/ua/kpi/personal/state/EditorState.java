package ua.kpi.personal.state;

public class EditorState implements BudgetAccessState {

    @Override
    public boolean canViewBudget() { return true; }

    @Override
    public boolean canAddTransaction() { return true; }

    /** Редактор може редагувати та видаляти будь-які фінансові дані. */
    @Override
    public boolean canModifyFinancialData() { return true; }

    /** Редактор не може керувати користувачами. */
    @Override
    public boolean canManageUsers() { return false; }

    /** Редактор не може видалити бюджет. */
    @Override
    public boolean canDeleteBudget() { return false; }

    @Override
    public String getDisplayRole() { return "Редактор"; }
}