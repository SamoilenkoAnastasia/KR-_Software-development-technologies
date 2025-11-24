package ua.kpi.personal.service;

import ua.kpi.personal.model.Transaction;
import ua.kpi.personal.model.analytics.MonthlyBalanceRow;
import ua.kpi.personal.model.analytics.CategoryReportRow;
import ua.kpi.personal.model.analytics.ReportParams;
import ua.kpi.personal.state.ApplicationSession;
import java.util.List;

public class AnalyticsService {
    
    private final ReportingService reportingService;
    private final ApplicationSession session;
    
    public AnalyticsService(ReportingService reportingService, ApplicationSession session) {
        this.reportingService = reportingService;
        this.session = session;
    }

    private Long validateAccessAndGetBudgetId() {
        if (!session.getCurrentBudgetAccessState().canViewBudget()) {
            throw new SecurityException("Помилка: Недостатньо прав для перегляду аналітики/звітів.");
        }
        Long budgetId = session.getCurrentBudgetId();
        if (budgetId == null) {
            throw new IllegalStateException("Помилка: Не обрано активний бюджет.");
        }
        return budgetId;
    }

    public double getNetWorth() {
        Long budgetId = validateAccessAndGetBudgetId(); 
        return reportingService.getTotalNetWorth(budgetId);
    }
    
    public List<Transaction> getTransactionsForReport(ReportParams params) {
        Long budgetId = validateAccessAndGetBudgetId(); 
        return reportingService.findTransactionsByDateRange(budgetId, params.getStartDate(), params.getEndDate());
    }
    
    public List<MonthlyBalanceRow> getMonthlyDynamicsReport(ReportParams params) {
        Long budgetId = validateAccessAndGetBudgetId(); 
        return reportingService.getMonthlyDynamics(params, budgetId);
    }

    public List<CategoryReportRow> getCategorySummaryReport(ReportParams params) {
        Long budgetId = validateAccessAndGetBudgetId(); 
        return reportingService.getCategorySummary(params, budgetId);
    }

}
