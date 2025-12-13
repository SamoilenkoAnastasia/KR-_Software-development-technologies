package ua.kpi.personal.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Transaction {
    
    private Long id;
    private Double amount;
    private String type;
    private String description;
    private String currency;
    private LocalDate transDate;
    private LocalDateTime createdAt;
    private Category category;
    private Account account;
    private Long accountId; 
    private User user;          
    private Long userId;        
    
    private User createdBy; 
    private Long budgetId;  
    private Long templateId;
    private Account originalAccount;
    private Double originalAmount;
    private String originalType;
    
    private Long categoryId;   

    public Long getId(){return id;}
    public void setId(Long id){this.id=id;}
    public Double getAmount(){return amount;}
    public void setAmount(Double amount){this.amount=amount;}
    public String getType(){return type;}
    public void setType(String type){this.type=type;}
    public String getDescription(){return description;}
    public void setDescription(String description){this.description=description;}
    public LocalDate getTransDate() {return transDate;}
    public void setTransDate(LocalDate transDate) {this.transDate = transDate;}
    public LocalDateTime getCreatedAt(){return createdAt;}
    public String getCurrency() {return currency;}
    public void setCurrency(String currency) {this.currency = currency;}
    public void setCreatedAt(LocalDateTime createdAt){this.createdAt=createdAt;}
    public Category getCategory(){return category;}
    public void setCategory(Category category){this.category=category;}
    public Account getAccount(){return account;}
    public void setAccount(Account account){this.account=account;}
    public User getUser(){return user;}
    public void setUser(User user){this.user=user;}
    
    
    public Long getUserId() {   
        if (userId != null) return userId;
        return user != null ? user.getId() : null;
    }

    public void setUserId(Long userId) {    
        this.userId = userId;
    }
    
    
    public Long getAccountId() {    
        if (accountId != null) return accountId;
        return account != null ? account.getId() : null;  
    }
    
   
    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }
    
    public Long getBudgetId() { return budgetId; }
    public void setBudgetId(Long budgetId) { this.budgetId = budgetId; }

    public boolean isIncome() {
        return "INCOME".equalsIgnoreCase(type);
    }
    
    public Long getTemplateId() { return templateId; }
    public void setTemplateId(Long templateId) { this.templateId = templateId; }
    
    public Account getOriginalAccount() { return originalAccount; }
    public void setOriginalAccount(Account originalAccount) { this.originalAccount = originalAccount; }
    public Double getOriginalAmount() { return originalAmount; }
    public void setOriginalAmount(Double originalAmount) { this.originalAmount = originalAmount; }
    public String getOriginalType() { return originalType; }
    public void setOriginalType(String originalType) { this.originalType = originalType; }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }
    
    public String getCreatedByName() {
        return createdBy != null && createdBy.getName() != null ? createdBy.getName() : "Система/Невідомо"; 
    }
    
    public Long getCategoryId() {
        if (categoryId == null && category != null) {
            return category.getId();
        }
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }
}