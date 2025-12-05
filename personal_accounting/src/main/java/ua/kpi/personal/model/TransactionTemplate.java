package ua.kpi.personal.model;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.DayOfWeek;
import java.util.Objects;
import java.util.Locale; 

public class TransactionTemplate implements Cloneable {
    private Long id;
    private String name;
    private String type;
    private Double defaultAmount;
    private Category category;
    private Account account;
    private User user;
    private String description; 
    private String currency = "UAH"; 
    private RecurringType recurringType = RecurringType.NONE; 
    private Integer recurrenceInterval = 1;      
    private LocalDate startDate;              
    private LocalDate lastExecutionDate;      
    private Integer dayOfMonth;               
    private DayOfWeek dayOfWeek;              

    public enum RecurringType {
        NONE, DAILY, WEEKLY, MONTHLY, YEARLY
    }

    public TransactionTemplate() {}

    @Override
    public TransactionTemplate clone() {
        try {
            return (TransactionTemplate) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError("CloneNotSupportedException не повинно виникати для TransactionTemplate.", e);
        }
    }
    
    public Transaction createTransactionFromTemplate(LocalDate executionDate) {
        Transaction tx = new Transaction();
        
        tx.setType(this.type);
        tx.setAmount(this.defaultAmount != null ? this.defaultAmount : 0.0);
        tx.setCreatedAt(executionDate.atTime(LocalDateTime.now().toLocalTime())); 
        tx.setCategory(this.category);
        tx.setAccount(this.account);
        tx.setUser(this.user);
        tx.setCurrency(this.currency);
        tx.setTemplateId(this.id);
        
        String baseDescription = this.description != null ? this.description : this.name;
        tx.setDescription(baseDescription + " (Автомат. платіж)"); 
        
        return tx;
    }
    
    public Transaction createTransactionFromTemplate() {
        return createTransactionFromTemplate(LocalDate.now());
    }

  
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Double getDefaultAmount() { return defaultAmount; }
    public void setDefaultAmount(Double defaultAmount) { this.defaultAmount = defaultAmount; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public Account getAccount() { return account; }
    public void setAccount(Account account) { this.account = account; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public RecurringType getRecurringType() { return recurringType; }
    public void setRecurringType(String recurringType) {
        if (recurringType == null || recurringType.isEmpty()) {
            this.recurringType = RecurringType.NONE;
            return;
        }
        try {
            this.recurringType = RecurringType.valueOf(recurringType.toUpperCase());
        } catch (IllegalArgumentException e) {
            this.recurringType = RecurringType.NONE;
        }
    }
    public void setRecurringType(RecurringType recurringType) { 
        this.recurringType = recurringType; 
    }
    
    public Integer getRecurrenceInterval() { return recurrenceInterval; }

    public void setRecurrenceInterval(Integer recurrenceInterval) {
        if (this.recurringType == RecurringType.NONE || recurrenceInterval == null) {
            this.recurrenceInterval = recurrenceInterval;
        } else {           
            this.recurrenceInterval = (recurrenceInterval.intValue() < 1) ? 1 : recurrenceInterval;
        }
    }
    
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getLastExecutionDate() { return lastExecutionDate; }
    public void setLastExecutionDate(LocalDate lastExecutionDate) { this.lastExecutionDate = lastExecutionDate; }
    public Integer getDayOfMonth() { return dayOfMonth; }
    public void setDayOfMonth(Integer dayOfMonth) { this.dayOfMonth = dayOfMonth; }
    public DayOfWeek getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(String dayOfWeek) {
        if (dayOfWeek == null || dayOfWeek.isEmpty()) {
            this.dayOfWeek = null;
            return;
        }
        try {
             this.dayOfWeek = DayOfWeek.valueOf(dayOfWeek.toUpperCase());
        } catch (IllegalArgumentException e) {
             this.dayOfWeek = null;
        }
    }
    public void setDayOfWeek(DayOfWeek dayOfWeek) { this.dayOfWeek = dayOfWeek; }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TransactionTemplate that = (TransactionTemplate) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}