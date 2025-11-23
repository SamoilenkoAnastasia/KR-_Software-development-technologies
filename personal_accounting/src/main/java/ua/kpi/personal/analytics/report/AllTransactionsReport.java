package ua.kpi.personal.analytics.report;

import ua.kpi.personal.model.User;
import ua.kpi.personal.model.Transaction;
import ua.kpi.personal.model.analytics.ReportParams;
import ua.kpi.personal.model.analytics.ReportDataPoint;
import ua.kpi.personal.repo.TransactionDao;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Звіт, що відображає всі транзакції за період.
 */
public class AllTransactionsReport extends FinancialReport {

    public AllTransactionsReport(TransactionDao transactionDao) {
        super(transactionDao);
    }

    // ВИПРАВЛЕНО: analyze повертає List<ReportDataPoint>
    @Override
    protected List<ReportDataPoint> analyze(ReportParams params, User user) {
        if (transactionDao == null) {
             throw new IllegalStateException("TransactionDao не встановлено.");
        }
        
        List<Transaction> transactions = transactionDao.findTransactionsByDateRange(params, user.getId());
        
        // Трансформація у List<ReportDataPoint> для універсального рендерингу
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
    
    // ВИПРАВЛЕНО: Реалізація render
    @Override
    protected void render(List<ReportDataPoint> dataPoints) {
        // Обчислення підсумку з використанням даних з dataPoints
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

        // Виклик уніфікованого методу рендерера
        this.renderer.renderReport(reportTitle, dataPoints, summary);
    }
}