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
    
    // !!! ДОДАНО !!!
    /**
     * Визначає, чи є користувач власником (Господарем) бюджету.
     * Господар має найвищі права, включаючи керування користувачами та видалення бюджету.
     */
    default boolean isOwner() {
        return canManageUsers() && canDeleteBudget();
    }
}