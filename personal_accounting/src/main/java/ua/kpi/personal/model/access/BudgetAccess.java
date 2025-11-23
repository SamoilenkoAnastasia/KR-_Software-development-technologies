package ua.kpi.personal.model.access;

public class BudgetAccess {

    private Long id;
    private Long budgetId; // ID спільного бюджету
    private Long userId;   // ID користувача, який має доступ
    
    // ? Роль (String), яку мапимо на об'єкти BudgetAccessState
    private String accessRole; 

    // --- КОНСТАНТИ ДЛЯ РОЛЕЙ ---
    public static final String ROLE_OWNER = "OWNER";
    public static final String ROLE_EDITOR = "EDITOR";
    public static final String ROLE_VIEWER = "VIEWER";


    public BudgetAccess() {
    }

    // --- Геттери та Сеттери ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBudgetId() {
        return budgetId;
    }

    public void setBudgetId(Long budgetId) {
        this.budgetId = budgetId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getAccessRole() {
        return accessRole;
    }

    public void setAccessRole(String accessRole) {
        this.accessRole = accessRole;
    }
}