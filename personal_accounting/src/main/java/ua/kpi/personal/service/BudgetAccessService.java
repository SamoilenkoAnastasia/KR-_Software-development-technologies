package ua.kpi.personal.service;

import ua.kpi.personal.state.BudgetAccessState;
import ua.kpi.personal.state.EditorState;
import ua.kpi.personal.state.NoAccessState;
import ua.kpi.personal.state.OwnerState;
import ua.kpi.personal.state.ViewerState;
import ua.kpi.personal.model.User;
import ua.kpi.personal.model.access.BudgetAccess;
import ua.kpi.personal.model.access.SharedBudget;
import ua.kpi.personal.repo.BudgetAccessDao;
import ua.kpi.personal.repo.SharedBudgetDao;
import ua.kpi.personal.state.ApplicationSession;
import java.util.List;

public class BudgetAccessService {

    private final BudgetAccessDao accessDao;
    private final SharedBudgetDao budgetDao;
    private final ApplicationSession session; // Посилання на сесію

    // ? ВИПРАВЛЕНО (1): Конструктор приймає 3 аргументи
    public BudgetAccessService(BudgetAccessDao accessDao, SharedBudgetDao budgetDao, ApplicationSession session) {
        this.accessDao = accessDao;
        this.budgetDao = budgetDao;
        this.session = session;
    }

    /**
     * Повертає список об'єктів BudgetAccess для всіх членів даного бюджету.
     */
    public List<BudgetAccess> findMembersByBudgetId(Long budgetId) {
        return accessDao.findMembersByBudgetId(budgetId);
    }


    public BudgetAccessState determineAccessState(Long budgetId, Long userId) {

        BudgetAccess access = accessDao.findAccessByBudgetAndUser(budgetId, userId);

        if (access == null) {
            return new NoAccessState();
        }

        switch (access.getAccessRole()) {
            case BudgetAccess.ROLE_OWNER:
                return new OwnerState();
            case BudgetAccess.ROLE_EDITOR:
                return new EditorState();
            case BudgetAccess.ROLE_VIEWER:
                return new ViewerState();
            default:
                return new NoAccessState();
        }
    }
    

    public void switchActiveBudget(Long budgetId) {
        User currentUser = session.getCurrentUser();

        if (currentUser == null) {
            System.err.println("Помилка: Користувач не авторизований. Перемикання бюджету неможливе.");
            return;
        }

        BudgetAccessState newState = determineAccessState(budgetId, currentUser.getId());

        session.setCurrentBudgetId(budgetId);
        // setCurrentBudgetAccessState оновлює UI в MainController
        session.setCurrentBudgetAccessState(newState); 

        System.out.printf(
            "Сесія оновлена: BudgetID=%d, Роль Доступу: %s\n",
            budgetId,
            newState.getDisplayRole()
        );
    }


    public SharedBudget createSharedBudget(String name, Long ownerId) {
        SharedBudget budget = new SharedBudget();
        budget.setName(name);
        budget.setOwnerId(ownerId);

        budget = budgetDao.create(budget);

        if (budget != null) {
            // Одразу додаємо власника до таблиці доступу
            BudgetAccess ownerAccess = new BudgetAccess();
            ownerAccess.setBudgetId(budget.getId());
            ownerAccess.setUserId(ownerId);
            ownerAccess.setAccessRole(BudgetAccess.ROLE_OWNER);
            accessDao.save(ownerAccess);

            // Після створення перемикаємо на нього сесію
            switchActiveBudget(budget.getId());
        }
        return budget;
    }


    public boolean addOrUpdateMember(Long budgetId, Long targetUserId, String role) {
        // Перевірка прав: чи має поточний користувач право керувати
        if (!ApplicationSession.getInstance().getCurrentBudgetAccessState().canManageUsers()) {
            System.err.println("Помилка: Недостатньо прав для управління членами бюджету.");
            return false;
        }

        // Оновлення або створення запису доступу
        BudgetAccess access = accessDao.findAccessByBudgetAndUser(budgetId, targetUserId);

        if (access == null || access.getBudgetId() == null) {
            // Створення нового запису
            access = new BudgetAccess();
            access.setBudgetId(budgetId);
            access.setUserId(targetUserId);
        }

        access.setAccessRole(role);

        return accessDao.save(access) != null;
    }


    public boolean removeMember(Long budgetId, Long targetUserId) {

        if (!ApplicationSession.getInstance().getCurrentBudgetAccessState().canManageUsers()) {
            System.err.println("Помилка: Недостатньо прав для видалення членів бюджету.");
            return false;
        }

        SharedBudget budget = budgetDao.findById(budgetId);
        if (budget != null && budget.getOwnerId().equals(targetUserId)) {
            System.err.println("Помилка: Не можна видалити власника бюджету.");
            return false;
        }

        return accessDao.delete(budgetId, targetUserId);
    }
}