package ua.kpi.personal.analytics.report;

import ua.kpi.personal.model.User;
import ua.kpi.personal.model.analytics.MonthlyBalanceRow;
import ua.kpi.personal.model.analytics.ReportDataPoint;
import ua.kpi.personal.model.analytics.ReportParams;
import ua.kpi.personal.service.AnalyticsService; 

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;


public class MonthlyDynamicsReport extends FinancialReport {

    private final AnalyticsService analyticsService; 

    public MonthlyDynamicsReport(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @Override
    protected List<ReportDataPoint> analyze(ReportParams params, User user) {

        List<MonthlyBalanceRow> monthlyDynamics = analyticsService.getMonthlyDynamicsReport(params);
        
        return monthlyDynamics.stream()
                .map(row -> new ReportDataPoint(
                        row.monthYear(), 
                        row.totalIncome(),
                        row.totalExpense(), 
                        "Динаміка",
                        row.monthYear().length() == 7 ? LocalDate.parse(row.monthYear() + "-01") : null
                ))
                .collect(Collectors.toList());
    }

    
    @Override
    protected void render(List<ReportDataPoint> dataPoints) {
        
        double totalIncome = dataPoints.stream().mapToDouble(ReportDataPoint::getValue).sum();
        double totalExpense = dataPoints.stream().mapToDouble(ReportDataPoint::getSecondaryValue).sum();
        double netBalance = totalIncome - totalExpense;

        String reportTitle = "Місячна Динаміка Доходів та Витрат";
        
        String summary = String.format("Загальний дохід: %.2f UAH, Загальні витрати: %.2f UAH, Чистий залишок: %.2f UAH", 
                                        totalIncome, totalExpense, netBalance);

        this.renderer.renderReport(reportTitle, dataPoints, summary);
    }
}
