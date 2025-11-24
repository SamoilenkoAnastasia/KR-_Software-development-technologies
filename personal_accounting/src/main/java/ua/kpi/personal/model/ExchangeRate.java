package ua.kpi.personal.model;

public class ExchangeRate {
    private String r030;       
    private String txt;        
    private double rate;      
    private String cc;         
    private String exchangedate; 

 
    public ExchangeRate() {}

    public String getCc() {
        return cc;
    }

    public double getRate() {
        return rate;
    }

    public String getTxt() {
        return txt;
    }

    public String getExchangedate() {
        return exchangedate;
    }
}