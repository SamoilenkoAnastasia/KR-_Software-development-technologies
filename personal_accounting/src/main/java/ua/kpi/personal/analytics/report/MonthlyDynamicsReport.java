package ua.kpi.personal.analytics.report;

import ua.kpi.personal.model.User;
import ua.kpi.personal.model.analytics.MonthlyBalanceRow;
import ua.kpi.personal.model.analytics.ReportDataPoint;
import ua.kpi.personal.model.analytics.ReportParams;
import ua.kpi.personal.repo.TransactionDao;
import ua.kpi.personal.service.ReportingService;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Звіт про динаміку доходів та витрат по місяцях.
 */
public class MonthlyDynamicsReport extends FinancialReport {

    private final ReportingService reportingService;

    public MonthlyDynamicsReport(TransactionDao dao, ReportingService reportingService) {
        super(dao);
        this.reportingService = reportingService;
    }

    // ВИПРАВЛЕНО: analyze повертає List<ReportDataPoint>
    @Override
    protected List<ReportDataPoint> analyze(ReportParams params, User user) {
        // ? ВИПРАВЛЕННЯ: Передаємо ID користувача (Long) замість об'єкта User
        List<MonthlyBalanceRow> monthlyDynamics = reportingService.getMonthlyDynamics(params, user.getId());
        
        return monthlyDynamics.stream()
                .map(row -> new ReportDataPoint(
                        row.monthYear(), 
                        row.totalIncome(),
                        row.totalExpense(), // secondaryValue
                        "Динаміка",
                        // Створюємо LocalDate для графіка
                        row.monthYear().length() == 7 ? LocalDate.parse(row.monthYear() + "-01") : null
                ))
                .collect(Collectors.toList());
    }

    // ВИПРАВЛЕНО: Реалізація render
    @Override
    protected void render(List<ReportDataPoint> dataPoints) {
        // Обчислення підсумку
        double totalIncome = dataPoints.stream().mapToDouble(ReportDataPoint::getValue).sum();
        double totalExpense = dataPoints.stream().mapToDouble(ReportDataPoint::getSecondaryValue).sum();
        double netBalance = totalIncome - totalExpense;

        String reportTitle = "Місячна Динаміка Доходів та Витрат";
        
        String summary = String.format("Загальний дохід: %.2f UAH, Загальні витрати: %.2f UAH, Чистий залишок: %.2f UAH", 
                                       totalIncome, totalExpense, netBalance);

        this.renderer.renderReport(reportTitle, dataPoints, summary);
    }
}