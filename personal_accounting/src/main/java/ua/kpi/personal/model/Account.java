package ua.kpi.personal.model;
import java.util.Objects;

public class Account {
    private Long id;
    private User user; // Власник акаунта (актуально для приватних акаунтів)
    private String name;
    private Double balance;
    private String type;
    private String currency;
    
    // ? ДОДАНО: Зв'язок із бюджетом. Account тепер може належати або User, або SharedBudget
    private Long budgetId; 
    
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
    
    // ? Геттер та Сеттер для budgetId
    public Long getBudgetId() { return budgetId; }
    public void setBudgetId(Long budgetId) { this.budgetId = budgetId; }
    
@Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Account account = (Account) o;
        // Порівняння лише за ID (якщо ID null, порівнюємо посилання)
        return Objects.equals(id, account.id);
    }

    @Override
    public int hashCode() {
        // Генеруємо хеш лише з ID
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        // Оптимізоване відображення для ChoiceBox: "Назва (Баланс Валюта)"
        String balanceStr = (balance != null) ? String.format("%.2f", balance) : "0.00";
        // ? Додано ID для налагодження, але можна залишити як було
        return String.format("%s (%.2f %s, ID:%d)", name, balance, currency, id);
    }
}
