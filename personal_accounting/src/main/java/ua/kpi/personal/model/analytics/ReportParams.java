package ua.kpi.personal.model.analytics;

import java.time.LocalDate;
import java.util.List;
import ua.kpi.personal.model.Category;
import ua.kpi.personal.model.Account;

public class ReportParams {
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final List<Category> categories;
    private final List<Account> accounts;
    private final String transactionType;

    public ReportParams(LocalDate startDate, LocalDate endDate, List<Category> categories, List<Account> accounts, String transactionType) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.categories = categories;
        this.accounts = accounts;
        this.transactionType = transactionType;
    }
    
    // Геттери...
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public List<Category> getCategories() { return categories; }
    public List<Account> getAccounts() { return accounts; }
    public String getTransactionType() { return transactionType; }
}