package ua.kpi.personal.model;

import java.util.Date;

public class Goal {
    
    private Long id;
    private User user; // Зв'язок з користувачем (власником цілі)
    private String name;
    private Double targetAmount; // Цільова сума
    private Double currentAmount; // Поточний зібраний баланс
    private String currency; // Валюта цілі (як і в Account)
    private Date deadline; // Кінцевий термін (Date або LocalDate/Time, вибираємо Date для простоти в DAO)
    private Boolean isFamilyFund; // Вимога ТЗ: ведення єдиного фонду (якщо true, доступна всім членам сім'ї/фонду)

    // Конструктор за замовчуванням
    public Goal() {
        this.currentAmount = 0.0;
        this.isFamilyFund = false;
    }
    
    // --- Геттери та Сеттери ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getTargetAmount() {
        return targetAmount;
    }

    public void setTargetAmount(Double targetAmount) {
        this.targetAmount = targetAmount;
    }

    public Double getCurrentAmount() {
        return currentAmount;
    }

    public void setCurrentAmount(Double currentAmount) {
        this.currentAmount = currentAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Date getDeadline() {
        return deadline;
    }

    public void setDeadline(Date deadline) {
        this.deadline = deadline;
    }

    public Boolean getIsFamilyFund() {
        return isFamilyFund;
    }

    public void setIsFamilyFund(Boolean isFamilyFund) {
        this.isFamilyFund = isFamilyFund;
    }
    
    @Override
    public String toString() {
        // Для відображення у ChoiceBox/ListView
        return String.format("%s (Ціль: %.2f %s. Зібрано: %.2f)", 
            name, targetAmount, currency, currentAmount);
    }
}