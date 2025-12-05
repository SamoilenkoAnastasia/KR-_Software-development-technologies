package ua.kpi.personal.model;
import java.util.Locale;
import java.util.Objects;

public class Account {
    private Long id;
    private User user; 
    private String name;
    private Double balance;
    private String type;
    private String currency;
    
    private Long budgetId; 

    private boolean isShared = false; 
    private Long ownerId; 
    
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
    

    public Long getBudgetId() { return budgetId; }
    public void setBudgetId(Long budgetId) { this.budgetId = budgetId; }
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
        String sharedMark = isShared ? " [Ρο³λόνθι]" : "";
        if (balance != null) {
            String balanceStr = String.format(Locale.US, "%.2f", balance);
            return String.format("%s%s (%s %s, ID:%d)", name, sharedMark, balanceStr, currency, id);
        } else if (id != null) {
            return String.format("%s%s (%s, ID:%d)", name, sharedMark, currency, id);
        } else {
             return String.format("%s%s (%s)", name, sharedMark, currency);
        }
    }
}