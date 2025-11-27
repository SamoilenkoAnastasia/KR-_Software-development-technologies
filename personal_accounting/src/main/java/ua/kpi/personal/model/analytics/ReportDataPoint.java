package ua.kpi.personal.model.analytics;

import java.time.LocalDate;

public class ReportDataPoint {
    private final String key;          
    private final double value;        
    private final double secondaryValue; 
    private final String label;        
    private final LocalDate date;      

    
    public ReportDataPoint(String key, double value, double secondaryValue, String label, LocalDate date) {
        this.key = key;
        this.value = value;
        this.secondaryValue = secondaryValue;
        this.label = label;
        this.date = date;
    }
    
   
    public ReportDataPoint(String key, double value, double secondaryValue, String label) {
        this(key, value, secondaryValue, label, null);
    }
    
    
    public ReportDataPoint(String key, double value, String label) {
        this(key, value, 0.0, label, null);
    }
    
    public String getKey() { return key; }
    public double getValue() { return value; }
    public double getSecondaryValue() { return secondaryValue; }
    public String getLabel() { return label; }
    public LocalDate getDate() { return date; }
}