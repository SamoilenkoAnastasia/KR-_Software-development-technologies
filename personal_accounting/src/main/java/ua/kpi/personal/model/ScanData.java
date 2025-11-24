package ua.kpi.personal.model;

import java.time.LocalDate;


public class ScanData {
    private String vendor;
    private double amount;
    private LocalDate date;
    private String recognizedText;
    private String suggestedCategoryName;

    public ScanData(String vendor, double amount, LocalDate date, String recognizedText, String suggestedCategoryName) {
        this.vendor = vendor;
        this.amount = amount;
        this.date = date;
        this.recognizedText = recognizedText;
        this.suggestedCategoryName = suggestedCategoryName;
    }

    public String getVendor() { return vendor; }
    public double getAmount() { return amount; }
    public LocalDate getDate() { return date; }
    public String getRecognizedText() { return recognizedText; }
    public String getSuggestedCategoryName() { return suggestedCategoryName; }
    
    public void setAmount(double amount) { this.amount = amount; }
    public void setVendor(String vendor) { this.vendor = vendor; }
    public void setSuggestedCategoryName(String suggestedCategoryName) { this.suggestedCategoryName = suggestedCategoryName; }

    public void setDate(LocalDate date) { this.date = date; } 
}