package ua.kpi.personal.analytics.report;

import ua.kpi.personal.model.User;
import ua.kpi.personal.model.analytics.CategoryReportRow;
import ua.kpi.personal.model.analytics.ReportDataPoint;
import ua.kpi.personal.model.analytics.ReportParams;
import ua.kpi.personal.service.ReportingService;

import java.util.List;
import java.util.stream.Collectors;
import java.time.LocalDate; // Додано для коректного форматування дати у summary

/**
 * Клас, що генерує звіт зі зведеною динамікою доходів та витрат за категоріями.
 */
public class CategoryReport extends FinancialReport { // ВИПРАВЛЕНО: extends замість implements

    private final ReportingService reportingService;

    public CategoryReport(ReportingService reportingService) {
        // Викликаємо конструктор базового класу без DAO
        super(); 
        this.reportingService = reportingService;
    }

    // ВИПРАВЛЕНО: Реалізація абстрактного методу analyze
    @Override
    protected List<ReportDataPoint> analyze(ReportParams params, User user) {
        List<CategoryReportRow> rawData = reportingService.getCategorySummary(params, user);

        return rawData.stream()
                .map(row -> new ReportDataPoint(
                        row.categoryName(), 
                        // Використовуємо абсолютне значення для коректного відображення PieChart
                        Math.abs(row.totalAmount()), 
                        0.0,
                        row.totalAmount() < 0 ? "Витрати" : (row.totalAmount() > 0 ? "Доходи/Баланс" : "Нуль")
                ))
                .collect(Collectors.toList());
    }

    // ВИПРАВЛЕНО: Реалізація абстрактного методу render
    @Override
    protected void render(List<ReportDataPoint> dataPoints) {
        if (renderer == null) {
            throw new IllegalStateException("Renderer не встановлено.");
        }
        
        // Розрахунок чистого балансу (враховуючи знак витрат)
        double totalBalance = dataPoints.stream()
                .mapToDouble(row -> row.getLabel().equals("Витрати") ? -row.getValue() : row.getValue()) 
                .sum();
        
        // Параметри звіту (потрібні для заголовка, але недоступні у цьому методі. 
        // Припускаємо, що title/summary будуть простими)
        String reportTitle = "Звіт за Категоріями"; 
        String summary = String.format("Чистий залишок: %.2f %s", totalBalance, "UAH");

        // ВИПРАВЛЕНО: Виклик уніфікованого методу рендерера
        this.renderer.renderReport(reportTitle, dataPoints, summary);
    }
}