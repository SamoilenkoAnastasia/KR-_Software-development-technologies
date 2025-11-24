package ua.kpi.personal.analytics.report;

import ua.kpi.personal.model.User;
import ua.kpi.personal.model.Transaction;
import ua.kpi.personal.model.analytics.ReportParams;
import ua.kpi.personal.model.analytics.ReportDataPoint;
import ua.kpi.personal.service.AnalyticsService; 
import java.util.List;
import java.util.stream.Collectors;

public class AllTransactionsReport extends FinancialReport {

    private final AnalyticsService analyticsService;

    // КОНСТРУКТОР З AnalyticsService
    public AllTransactionsReport(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @Override
    protected List<ReportDataPoint> analyze(ReportParams params, User user) {
        
        // ЦЕЙ ВИКЛИК ТЕПЕР ПРАЦЮЄ
        List<Transaction> transactions = analyticsService.getTransactionsForReport(params); 
        
        return transactions.stream()
                .map(t -> new ReportDataPoint(
                        // Key
                        t.getDescription() != null ? t.getDescription() : "Транзакція " + t.getId().toString(), 
                        // Value
                        t.getAmount(),
                        // SecondaryValue
                        0.0,
                        // Label
                        String.format("%s (%s)", 
                                    t.getType(), 
                                    t.getCategory() != null ? t.getCategory().getName() : "Без категорії"),
                        // Date
                        t.getCreatedAt().toLocalDate()
                ))
                .collect(Collectors.toList());
    }
    
    @Override
    protected void render(List<ReportDataPoint> dataPoints) {
        // Обчислення підсумку з використанням даних з dataPoints (логіка не змінюється)
        double totalIncome = dataPoints.stream()
                .filter(dp -> dp.getLabel().startsWith("INCOME"))
                .mapToDouble(ReportDataPoint::getValue)
                .sum();
                
        double totalExpense = dataPoints.stream()
                .filter(dp -> dp.getLabel().startsWith("EXPENSE"))
                .mapToDouble(ReportDataPoint::getValue)
                .sum();
                
        double netBalance = totalIncome - totalExpense;

        String reportTitle = "Детальний Звіт по Транзакціях";
        String summary = String.format("Загальний дохід: %.2f UAH, Загальні витрати: %.2f UAH, Чистий залишок: %.2f UAH", 
                                        totalIncome, totalExpense, netBalance);

        this.renderer.renderReport(reportTitle, dataPoints, summary);
    }
}
