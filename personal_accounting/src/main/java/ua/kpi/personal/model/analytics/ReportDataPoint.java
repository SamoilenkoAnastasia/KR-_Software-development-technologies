package ua.kpi.personal.model.analytics;

import java.time.LocalDate;

/**
 * Універсальний DTO для однієї точки даних у звіті (таблиці або графіку).
 */
public class ReportDataPoint {
    private final String key;          // Основний ключ (наприклад, Назва Категорії, Місяць)
    private final double value;        // Основне значення (наприклад, Сума витрат/доходів)
    private final double secondaryValue; // Додаткове значення (наприклад, Дохід для звіту про витрати)
    private final String label;        // Мітка (наприклад, "Витрати", "Дохід")
    private final LocalDate date;      // Дата або початкова дата періоду

    // Конструктор для динамічних звітів (місячна динаміка)
    public ReportDataPoint(String key, double value, double secondaryValue, String label, LocalDate date) {
        this.key = key;
        this.value = value;
        this.secondaryValue = secondaryValue;
        this.label = label;
        this.date = date;
    }
    
    // Конструктор для звітів за категоріями (де дата не потрібна)
    public ReportDataPoint(String key, double value, double secondaryValue, String label) {
        this(key, value, secondaryValue, label, null);
    }
    
    // Конструктор для простих звітів (де потрібен лише ключ і одне значення)
    public ReportDataPoint(String key, double value, String label) {
        this(key, value, 0.0, label, null);
    }
    
    public String getKey() { return key; }
    public double getValue() { return value; }
    public double getSecondaryValue() { return secondaryValue; }
    public String getLabel() { return label; }
    public LocalDate getDate() { return date; }
}