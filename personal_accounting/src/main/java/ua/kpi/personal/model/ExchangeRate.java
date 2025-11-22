package ua.kpi.personal.model;

public class ExchangeRate {
    private String r030;       // Код валюти (наприклад, 840)
    private String txt;        // Назва валюти (наприклад, Долар США)
    private double rate;       // Курс
    private String cc;         // Символ валюти (наприклад, USD)
    private String exchangedate; // Дата курсу

    // Конструктор за замовчуванням
    public ExchangeRate() {}

    // Геттери, необхідні для ExchangeRateService
    public String getCc() {
        return cc;
    }

    public double getRate() {
        return rate;
    }

    // Решта геттерів/сеттерів (можна додати за потреби)
    public String getTxt() {
        return txt;
    }

    public String getExchangedate() {
        return exchangedate;
    }
}