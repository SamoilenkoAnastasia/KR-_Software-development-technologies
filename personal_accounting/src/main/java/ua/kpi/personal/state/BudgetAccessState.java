package ua.kpi.personal.state;

/**
 * Інтерфейс, що визначає, які дії дозволені користувачу в поточному стані бюджету.
 */
public interface BudgetAccessState {

    /**
     * Перевіряє дозвіл на перегляд бюджету.
     */
    boolean canViewBudget();

    /**
     * Перевіряє дозвіл на додавання нових транзакцій.
     */
    boolean canAddTransaction();

    /**
     * Перевіряє дозвіл на зміну або видалення існуючих фінансових даних (рахунки, категорії).
     */
    boolean canModifyFinancialData();

    /**
     * Перевіряє дозвіл на керування користувачами (запрошення, зміна ролей).
     */
    boolean canManageUsers();

    /**
     * Перевіряє дозвіл на видалення бюджету.
     */
    boolean canDeleteBudget();

    /**
     * Повертає назву ролі для відображення в UI.
     */
    String getDisplayRole();

    /**
     * Уніфікована перевірка дозволу на будь-яке редагування фінансових даних (транзакції або модифікації).
     * ВИПРАВЛЕНО: Перейменовано з canEditFinance() на canEdit() для відповідності MainController.
     * @return true, якщо дозволено додавати транзакції або змінювати дані.
     */
    default boolean canEdit() {
        return canAddTransaction() || canModifyFinancialData();
    }
}