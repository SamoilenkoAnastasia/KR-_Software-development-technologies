package ua.kpi.personal.analytics.report;

import ua.kpi.personal.model.User;
import ua.kpi.personal.model.analytics.CategoryReportRow;
import ua.kpi.personal.model.analytics.ReportDataPoint;
import ua.kpi.personal.model.analytics.ReportParams;
import ua.kpi.personal.service.ReportingService;

import java.util.List;
import java.util.stream.Collectors;
import java.time.LocalDate; 

/**
 * Клас, що генерує звіт зі зведеною динамікою доходів та витрат за категоріями.
 */
public class CategoryReport extends FinancialReport {

    private final ReportingService reportingService;

    public CategoryReport(ReportingService reportingService) {
        // Оскільки FinancialReport очікує DAO, інакше вам потрібен конструктор за замовчуванням
        // АБО передайте null, якщо DAO не використовується в базовому класі, але це ризиковано.
        // Припускаємо, що базовий конструктор дозволяє null або ви його перевизначили.
        // Якщо конструктор FinancialReport вимагає TransactionDao, вам потрібно його змінити або отримати.
        super(null); // Припускаємо, що базовий конструктор дозволяє null або має інший сигнатуру.
        this.reportingService = reportingService;
    }

    // ВИПРАВЛЕНО: Реалізація абстрактного методу analyze
    @Override
    protected List<ReportDataPoint> analyze(ReportParams params, User user) {
        // ? ВИПРАВЛЕННЯ: Передаємо ID користувача (Long) замість об'єкта User
        List<CategoryReportRow> rawData = reportingService.getCategorySummary(params, user.getId());

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
                // Перевірка мітки для визначення знаку: витрати завжди повинні бути від'ємними для чистого балансу
                .mapToDouble(row -> row.getLabel().equals("Витрати") ? -row.getValue() : row.getValue()) 
                .sum();
        
        // Виведення результатів
        String reportTitle = "Звіт за Категоріями"; 
        String summary = String.format("Чистий залишок: %.2f %s", totalBalance, "UAH");

        this.renderer.renderReport(reportTitle, dataPoints, summary);
    }
}