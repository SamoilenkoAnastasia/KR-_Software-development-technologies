package ua.kpi.personal.analytics.report;

import ua.kpi.personal.model.User;
import ua.kpi.personal.model.analytics.CategoryReportRow;
import ua.kpi.personal.model.analytics.ReportDataPoint;
import ua.kpi.personal.model.analytics.ReportParams;
import ua.kpi.personal.service.AnalyticsService; // <-- НОВА ЗАЛЕЖНІСТЬ

import java.util.List;
import java.util.stream.Collectors;

public class CategoryReport extends FinancialReport {

    private final AnalyticsService analyticsService; // <-- НОВА ЗАЛЕЖНІСТЬ

    // КОНСТРУКТОР З AnalyticsService
    public CategoryReport(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @Override
    protected List<ReportDataPoint> analyze(ReportParams params, User user) {
        
        // ВИКЛИК БЕЗПЕЧНОГО МЕТОДУ: analyticsService перевіряє права та підставляє budgetId
        List<CategoryReportRow> rawData = analyticsService.getCategorySummaryReport(params);

        return rawData.stream()
                .map(row -> new ReportDataPoint(
                        row.categoryName(), 
                        Math.abs(row.totalAmount()), 
                        0.0,
                        // Мітка залежить від знаку:
                        row.totalAmount() < 0 ? "Витрати" : (row.totalAmount() > 0 ? "Доходи/Баланс" : "Нуль")
                ))
                .collect(Collectors.toList());
    }

    @Override
    protected void render(List<ReportDataPoint> dataPoints) {
        if (renderer == null) {
            throw new IllegalStateException("Renderer не встановлено.");
        }
            
        double totalBalance = dataPoints.stream()
                // Перетворюємо значення назад у правильний знак для підрахунку загального балансу
                .mapToDouble(row -> row.getLabel().equals("Витрати") ? -row.getValue() : row.getValue()) 
                .sum();

        String reportTitle = "Звіт за Категоріями"; 
        String summary = String.format("Чистий залишок: %.2f %s", totalBalance, "UAH");

        this.renderer.renderReport(reportTitle, dataPoints, summary);
    }
}