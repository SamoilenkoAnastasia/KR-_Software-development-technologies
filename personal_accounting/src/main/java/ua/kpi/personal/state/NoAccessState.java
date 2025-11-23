package ua.kpi.personal.state;

/**
 * Репрезентує стан, коли користувач не має жодних прав на взаємодію з бюджетом.
 */
public class NoAccessState implements BudgetAccessState {

    @Override
    public boolean canViewBudget() { return false; }

    @Override
    public boolean canAddTransaction() { return false; }

    /**
     * ВИПРАВЛЕНО: Додано реалізацію абстрактного методу з інтерфейсу.
     */
    @Override
    public boolean canModifyFinancialData() { return false; }

    @Override
    public boolean canManageUsers() { return false; }

    /**
     * ВИПРАВЛЕНО: Додано реалізацію абстрактного методу з інтерфейсу.
     */
    @Override
    public boolean canDeleteBudget() { return false; }

    @Override
    public String getDisplayRole() { return "Немає Доступу"; }

    // Примітка: Метод canEditData() було видалено, оскільки його функціональність
    // покривається default методом canEdit() в інтерфейсі BudgetAccessState.
}