package ua.kpi.personal.model.analytics;

public class ReportDataPoint {
    private final String key;       
    private final double value;     
    private final double secondaryValue; 

    public ReportDataPoint(String key, double value, double secondaryValue) {
        this.key = key;
        this.value = value;
        this.secondaryValue = secondaryValue;
    }

  
    public String getKey() { return key; }
    public double getValue() { return value; }
    public double getSecondaryValue() { return secondaryValue; }
}