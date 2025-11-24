package ua.kpi.personal.model;

import java.util.Date;

public class Goal {
    
    private Long id;
    private String name;
    private Double targetAmount; 
    private Double currentAmount; 
    private String currency; 
    private Date deadline; 
    private Long budgetId; 


    public Goal() {
        this.currentAmount = 0.0;
    }


    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Double getTargetAmount() { return targetAmount; }
    public void setTargetAmount(Double targetAmount) { this.targetAmount = targetAmount; }

    public Double getCurrentAmount() { return currentAmount; }
    public void setCurrentAmount(Double currentAmount) { this.currentAmount = currentAmount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public Date getDeadline() { return deadline; }
    public void setDeadline(Date deadline) { this.deadline = deadline; }
    
    public Long getBudgetId() { return budgetId; }
    public void setBudgetId(Long budgetId) { this.budgetId = budgetId; }

    
    @Override
    public String toString() {
        return String.format("%s (Ціль: %.2f %s. Зібрано: %.2f)", 
            name, targetAmount, currency, currentAmount);
    }
}