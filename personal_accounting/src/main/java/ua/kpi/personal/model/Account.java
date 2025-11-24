package ua.kpi.personal.model;
import java.util.Objects;

public class Account {
    private Long id;
    private User user; // Власник акаунта (актуально для приватних акаунтів)
    private String name;
    private Double balance;
    private String type;
    private String currency;
    
    private Long budgetId; 
    
    // --- НОВІ ПОЛЯ ДЛЯ СПІЛЬНОГО ДОСТУПУ ---
    private boolean isShared = false; // За замовчуванням рахунок приватний
    private Long ownerId; // ID власника для зручності DAO/бізнес-логіки
    // ---------------------------------------
    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    
    public Double getBalance() {
        return balance;
    }

    public void setBalance(Double balance) {
        this.balance = balance;
    }
    
    
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    
    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    
    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
    
    // Геттер та Сеттер для budgetId
    public Long getBudgetId() { return budgetId; }
    public void setBudgetId(Long budgetId) { this.budgetId = budgetId; }

    // --- НОВІ ГЕТТЕРИ/СЕТТЕРИ ---
    public boolean isShared() { return isShared; }
    public void setShared(boolean shared) { isShared = shared; }
    
    public Long getOwnerId() { 
        return ownerId != null ? ownerId : (user != null ? user.getId() : null); 
    }
    public void setOwnerId(Long ownerId) { 
        this.ownerId = ownerId; 
        if (this.user == null) {
            this.user = new User();
            this.user.setId(ownerId);
        }
    }
    // ------------------------------
    
@Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Account account = (Account) o;
        return Objects.equals(id, account.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        String balanceStr = (balance != null) ? String.format("%.2f", balance) : "0.00";
        // Додамо позначку спільності
        String sharedMark = isShared ? " [Спільний]" : "";
        return String.format("%s%s (%.2f %s, ID:%d)", name, sharedMark, balance, currency, id);
    }
}