package ua.kpi.personal.state;

public interface BudgetAccessState {

    boolean canViewBudget();

    boolean canAddTransaction();

    boolean canModifyFinancialData();

    boolean canManageUsers();

    boolean canDeleteBudget();

    String getDisplayRole();

    
    default boolean canEdit() {
        return canAddTransaction() || canModifyFinancialData();
    }
    
    default boolean isOwner() {
        return canManageUsers() && canDeleteBudget();
    }
}